
VER_LINE=`cat app/build.gradle | grep versionName`
echo $VER_LINE
VER_CODE=${VER_LINE#*\"}
VER_CODE=${VER_CODE%*\"}
echo $VER_CODE

cd ../

tar -cvjf JdSmartOpenSDK-${VER_CODE}.tar.bz2 JdSmartOpenSDK --exclude=.git --exclude=.gradle --exclude=JdSmartOpenSDK/app/build --exclude=.idea --exclude=JdSmartOpenSDK/build --exclude=pack.sh 

echo "output file:" ${PWD}/JdSmartOpenSDK-${VER_CODE}.tar.bz2

cd -

