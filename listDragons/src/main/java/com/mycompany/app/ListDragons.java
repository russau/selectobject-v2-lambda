// # Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// #
// # Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
// # except in compliance with the License. A copy of the License is located at
// #
// # https://aws.amazon.com/apache-2-0/
// #
// # or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
// # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
// # the specific language governing permissions and limitations under the License.

package com.mycompany.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompressionType;
import software.amazon.awssdk.services.s3.model.ExpressionType;
import software.amazon.awssdk.services.s3.model.InputSerialization;
import software.amazon.awssdk.services.s3.model.JSONInput;
import software.amazon.awssdk.services.s3.model.JSONOutput;
import software.amazon.awssdk.services.s3.model.OutputSerialization;
import software.amazon.awssdk.services.s3.model.SelectObjectContentEventStream;
import software.amazon.awssdk.services.s3.model.SelectObjectContentRequest;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponse;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponseHandler;
import software.amazon.awssdk.services.s3.model.selectobjectcontenteventstream.DefaultRecords;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

/**
 * Lambda function: ListDragons.
 */
public class ListDragons implements RequestHandler<APIGatewayProxyRequestEvent,
    APIGatewayProxyResponseEvent> {

  private static final SsmClient ssmClient = SsmClient.builder().build();
  private static final S3AsyncClient s3 = S3AsyncClient.builder().build();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input,
      Context context) {
    String dragonData = readDragonData(input);
    return generateResponse(dragonData);
  }

  protected static String readDragonData(APIGatewayProxyRequestEvent event) {
    Map<String, String> queryParams = event.getQueryStringParameters();
    String bucketName = getBucketName();
    String key = getKey();
    String query = getQuery(queryParams);
    JsonArray results = new JsonArray();

    TestHandler testHandler = new TestHandler();

    // See reference on AWS SDK for Java 2.x async programming
    // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/asynchronous.html
    CompletableFuture<Void> selected = queryS3(s3, bucketName, key, query, testHandler);
    selected.join();

    for (SelectObjectContentEventStream events : testHandler.receivedEvents) {
      if (events instanceof DefaultRecords) {
        DefaultRecords defaultRecords = (DefaultRecords) events;
        String payload = defaultRecords.payload().asUtf8String();
        Scanner scanner = new Scanner(payload);
        while (scanner.hasNextLine()) {
          JsonElement e = JsonParser.parseString(scanner.nextLine());
          results.add(e);
        }
        scanner.close();
      }
    }
    return results.toString();
  }

  private static String getBucketName() {
    GetParameterRequest getParameterRequest = GetParameterRequest
        .builder()
        .name("dragon_data_bucket_name")
        .withDecryption(false).build();
    GetParameterResponse getParameterResponse = ssmClient.getParameter(getParameterRequest);
    return getParameterResponse.parameter().value();
  }

  private static String getKey() {
    GetParameterRequest getParameterRequest = GetParameterRequest
        .builder()
        .name("dragon_data_file_name")
        .withDecryption(false).build();
    GetParameterResponse getParameterResponse = ssmClient.getParameter(getParameterRequest);
    return getParameterResponse.parameter().value();
  }

  private static String getQuery(Map<String, String> queryParams) {
    if (queryParams != null) {
      System.out.println("we have params");
      if (queryParams.containsKey("family")) {
        System.out.println(queryParams.get("family"));
        return "select * from S3Object[*][*] s where s.family_str =  '"
            + queryParams.get("family") + "'";

      } else if (queryParams.containsKey("dragonName")) {
        return "select * from S3Object[*][*] s where s.dragon_name_str =  '"
            + queryParams.get("dragonName") + "'";
      }
    }

    return "select * from s3object[*][*] s";
  }

  private static CompletableFuture<Void> queryS3(S3AsyncClient s3,
      String bucketName,
      String key,
      String query,
      SelectObjectContentResponseHandler handler) {
    InputSerialization inputSerialization = InputSerialization.builder()
        .json(JSONInput.builder().type("Document").build())
        .compressionType(CompressionType.NONE)
        .build();

    OutputSerialization outputSerialization = OutputSerialization.builder()
        .json(JSONOutput.builder().build())
        .build();

    SelectObjectContentRequest select = SelectObjectContentRequest.builder()
        .bucket(bucketName)
        .key(key)
        .expression(query)
        .expressionType(ExpressionType.SQL)
        .inputSerialization(inputSerialization)
        .outputSerialization(outputSerialization)
        .build();

    return s3.selectObjectContent(select, handler);
  }

  private static APIGatewayProxyResponseEvent generateResponse(String dragons) {
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("access-control-allow-origin", "*");

    response.setStatusCode(200);
    response.setBody(dragons);
    response.setHeaders(headers);
    return response;
  }

  private static class TestHandler implements SelectObjectContentResponseHandler {
    private List<SelectObjectContentEventStream> receivedEvents = new ArrayList<>();

    @Override
    public void responseReceived(SelectObjectContentResponse response) {
    }

    @Override
    public void onEventStream(SdkPublisher<SelectObjectContentEventStream> publisher) {
      publisher.subscribe(receivedEvents::add);
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
    }

    @Override
    public void complete() {
    }
  }
}
