rm -rf .local-repository
mkdir .local-repository

gradle clean
gradle :library:uploadArchives
gradle :library_external:uploadArchives