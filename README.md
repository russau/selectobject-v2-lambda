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
aws lambda invoke --function-name ListDragons5 ~/outfile.txt
```

``` bash
java -jar ~/Downloads/checkstyle-9.2.1-all.jar -c /google_checks.xml src/
```

``` bash
aws lambda update-function-code --function-name ListDragons \
--zip-file fileb://build/distributions/listDragons.zip
```

```
aws lambda create-function --function-name AddDragon \
--runtime java11 \
--role $ROLE_ARN_READWRITE \
--handler com.mycompany.app.AddDragon::handleRequest \
--publish \
--zip-file fileb://build/distributions/addDragon.zip
```
