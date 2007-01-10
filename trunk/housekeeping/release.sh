#!/bin/bash

# Using a shell script for now, because it's not clear that Maven's release plugin
# exactly fits the bill....

version="$1"
if [ ! -n "$version" ]; then
    echo "ERROR: specify version"
    exit 1
fi

sxml_home="$(dirname "$0")"/..
cd "$sxml_home"

if [ $(svn status | wc -l) -gt 0 ]; then
    svn status
    echo
    echo 'ERROR: uncommitted work'
    echo
    exit 1
fi

./housekeeping/bundle-dist.sh "$version" || exit 1

echo
echo "Ready to release version:   $version"
echo
echo -n "Press enter to proceed: "
read

cd "$sxml_home"

svn copy \
    . \
    "https://sweetxml.googlecode.com/svn/tags/release/$version" \
    -m "Tagging release $version" || exit 1

cd /tmp    
curl 'http://support.googlecode.com/svn/trunk/scripts/googlecode-upload.py' >/tmp/google-upload.py
for f in "sweetxml-$version.zip" "sweetxml-$version.tar.gz"; do
    python /tmp/google-upload.py -s "SweetXML $version" -p sweetxml -l Featured -u paul.cantrell "$f" || exit 1
done
