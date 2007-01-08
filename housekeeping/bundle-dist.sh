#!/bin/bash

version="$1"
if [ ! -n "$version" ]; then
    echo "ERROR: specify version"
    exit 1
fi

function die()
    {
    echo "BUNDLE FAILED: $1" >&2
    exit 1
    }
    
home="$PWD"

./housekeeping/checkversions.sh

echo "Bundling SweetXML $version..."

if grep '^    version:' */pom.sxml | grep -v "version: *$version *\$" > /dev/null; then
    die 'ERROR: poms have incorrect versions'
fi

for module in . core-java maven ant; do
    echo
    echo
    echo "Building $module..."
    echo
    cd "$home/$module" || die "No such dir $home/$module"
    mvn install || die "mvn failed"
    cd -
done

set -x

staging="/tmp/sweetxml-$version"
rm -rf "$staging"{,.tar.gz,.zip}
mkdir "$staging" || die "Can't create $staging"

cp -R doc/html/ "$staging/doc"      || die "Can't copy docs"
cp -R core-java/bin/ "$staging/bin" || die "Can't copy bin"
cp LICENSE.html "$staging"          || die "Can't copy license"

cp "core-java/build/sweetxml-$version.jar" "$staging/"   || die "can't copy jars"
cp "ant/build/sweetxml-ant-$version.jar" "$staging/"     || die "can't copy jars"
cp "maven/build/sweetxml-maven-$version.jar" "$staging/" || die "can't copy jars"

find "$staging" -name '.*' -exec rm -rf {} \;
cd /tmp
tar cvfz "sweetxml-$version.tar.gz" "sweetxml-$version" || die "can't create archive"
zip -v -r "sweetxml-$version.zip" "sweetxml-$version"   || die "can't create archive"

set +x
echo
echo "Bundled product is in $staging"
echo
