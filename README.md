Install gradle: https://docs.aws.amazon.com/cloud9/latest/user-guide/sample-java.html#sample-java-sdk-gradle

``` bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle
```



``` bash
aws lambda create-function --function-name ListDragons \
--runtime java11 \
--role $ROLE_ARN_READ \
--handler com.mycompany.app.ListDragons::handleRequest \
--publish \
--timeout 90 \
--memory-size 448 \
--zip-file fileb://build/distributions/listDragons.zip
```

``` bash
aws lambda invoke --function-name ListDragons ~/outfile.txt
```


``` bash
aws lambda update-function-code --function-name ListDragons \
--zip-file fileb://build/distributions/listDragons.zip
```

``` bash
aws lambda create-function --function-name AddDragon \
--runtime java11 \
--role $ROLE_ARN_READWRITE \
--handler com.mycompany.app.AddDragon::handleRequest \
--timeout 90 \
--memory-size 448 \
--publish \
--zip-file fileb://build/distributions/addDragon.zip
```

``` bash
aws lambda invoke --function-name AddDragon --payload fileb://newDragonPayload.json output.txt ; cat output.txt
```



Checkstyle stuff

``` bash
java -jar ~/Downloads/checkstyle-9.2.1-all.jar -c /google_checks.xml src/
```
