/*
 * The MIT License (MIT)
 * Copyright (c) 2019 SP Inspire Consulting Ltd and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

//Based on https://www.journaldev.com/13629/okhttp-android-example-tutorial

package org.neotree.support.okhttp;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class APIGatewayHelper {


    OkHttpClient client = new OkHttpClient();

    public String txtString;

    public String apiKey = "<api key goes here>";
    public String auth0token = "<auth 0 current token goes here with long exp for testing>";


    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public boolean getFromApi(ObjectNode requestPayload, String endpointUrl) {

        try {
            makeApiGETCall(endpointUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean postToApi(ObjectNode requestPayload, String endpointUrl, String BearerKey) {

        try {
            makeApiPOSTCall(endpointUrl, BearerKey, requestPayload.toString());
            int size = requestPayload.size();
            //makeApiPOSTCall(endpointUrl, postBody);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

     //ObjectNode payload

    public void makeApiGETCall(String url) throws IOException {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("Authorization", auth0token)
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String myResponse = response.body().string();

                txtString = myResponse;

            }
        });
    }

    public void makeApiPOSTCall(String postUrl, String authKey, String postBody) throws IOException {
        if (authKey != null) {
            if (authKey.length() > 1) {
                auth0token = authKey;
            }
        }
        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(JSON, postBody);

        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("Authorization", "Bearer " + auth0token)
                .url(postUrl)
                .post(body)
                .build();

        String APIKey = apiKey;


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //Log.d("TAG",response.body().string());
                txtString = response.toString();
            }
        });
    }




    // Write JSON output
     //   mapper.writeValue(exportFile, root);
   // }


}

