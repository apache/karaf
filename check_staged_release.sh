#!/bin/sh

STAGING=${1}
DOWNLOAD=${2:-/tmp/felix-staging}
mkdir ${DOWNLOAD} 2>/dev/null

# The following code automatically imports the signing KEYS, but it may actually be
# better to download them from a key server and/or let the user choose what keys
# he wants to import.
#wget --no-check-certificate -P "${DOWNLOAD}" http://www.apache.org/dist/felix/KEYS 
#gpg --import "${DOWNLOAD}/KEYS"

if [ -z "${STAGING}" -o ! -d "${DOWNLOAD}" ]
then
 echo "Usage: check_staged_release.sh <staging-number> [temp-directory]"
 exit
fi

if [ ! -e "${DOWNLOAD}/${STAGING}" ]
then
 echo "################################################################################"
 echo "                           DOWNLOAD STAGED REPOSITORY                           "
 echo "################################################################################"

 wget \
  -r -np "--reject=html,txt" "--follow-tags=" \
  -P "${DOWNLOAD}/${STAGING}" -nH "--cut-dirs=3" --ignore-length --no-check-certificate \
  "http://repository.apache.org/content/repositories/orgapachefelix-${STAGING}/org/apache/felix/"

else
 echo "################################################################################"
 echo "                       USING EXISTING STAGED REPOSITORY                         "
 echo "################################################################################"
 echo "${DOWNLOAD}/${STAGING}"
fi

echo "################################################################################"
echo "                          CHECK SIGNATURES AND DIGESTS                          "
echo "################################################################################"

for i in `find "${DOWNLOAD}/${STAGING}" -type f | grep -v '\.\(asc\|sha1\|md5\)$'`
do
 f=`echo $i | sed 's/\.asc$//'`
 echo "$f"
 gpg --verify $f.asc 2>/dev/null
 if [ "$?" = "0" ]; then CHKSUM="GOOD"; else CHKSUM="BAD!!!!!!!!"; fi
 if [ ! -f "$f.asc" ]; then CHKSUM="----"; fi
 echo "gpg:  ${CHKSUM}"
 if [ "`cat $f.md5 2>/dev/null`" = "`openssl md5 < $f 2>/dev/null`" ]; then CHKSUM="GOOD"; else CHKSUM="BAD!!!!!!!!"; fi
 if [ ! -f "$f.md5" ]; then CHKSUM="----"; fi
 echo "md5:  ${CHKSUM}"
 if [ "`cat $f.sha1 2>/dev/null`" = "`openssl sha1 < $f 2>/dev/null`" ]; then CHKSUM="GOOD"; else CHKSUM="BAD!!!!!!!!"; fi
 if [ ! -f "$f.sha1" ]; then CHKSUM="----"; fi
 echo "sha1: ${CHKSUM}"
done

if [ -z "${CHKSUM}" ]; then echo "WARNING: no files found!"; fi

echo "################################################################################"

