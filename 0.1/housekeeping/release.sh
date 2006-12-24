#!/bin/bash

# Using a shell script for now, because it's not clear that Maven's release plugin
# exactly fits the bill....

version="$1"
if [ ! -n "$version" ]; then
    echo "ERROR: specify version"
    exit 1
fi

cd "$(dirname "$0")"/..

svn status
if [ $(svn status | wc -l) -gt 0 ]; then
    echo
    echo 'ERROR: uncommitted work'
    echo
    exit 1
fi

./housekeeping/checkversions.sh

if grep '^    version:' */pom.sxml | grep -v "version: *$version *\$" > /dev/null; then
    echo
    echo 'ERROR: poms have incorrect versions'
    echo
    exit 1
fi

tag="release/$version"

for module in core-java maven; do
    echo
    echo
    echo "Building $module..."
    echo
    cd $module
    mvn clean package || exit 1
    cd -
done

staging="/tmp/sweetxml-$version"
rm -rf "$staging"{,.tar.gz,.zip}
mkdir "$staging" || exit 1

cp -R doc/html/ "$staging/doc" || exit 1
cp -R core-java/bin/ "$staging/bin" || exit 1
cp LICENSE.html "$staging" || exit 1

mkdir "$staging/build"
cp "core-java/build/sweetxml-$version.jar" "$staging/build" || exit 1

find "$staging" -name '.*' -exec rm -rf {} \;
cd /tmp
tar cvfz "sweetxml-$version.tar.gz" "sweetxml-$version" || exit 1
zip -v -r "sweetxml-$version.zip" "sweetxml-$version" || exit 1

echo
echo "Ready to release version:   $version"
echo
echo -n "Press enter to proceed: "
read

cd -
svn copy \
    . \
    "https://sweetxml.googlecode.com/svn/tags/release/$version" \
    -m "Tagging release $version" || exit 1

cd /tmp    
curl 'http://support.googlecode.com/svn/trunk/scripts/googlecode-upload.py' >/tmp/google-upload.py
for f in "sweetxml-$version.zip" "sweetxml-$version.tar.gz"; do
    python /tmp/google-upload.py -s "SweetXML $version" -p sweetxml -l Featured -u paul.cantrell "$f" || exit 1
done
