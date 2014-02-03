/*
 *
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.netflix.ribbon.examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.concurrent.Future;


import com.google.gson.Gson;
import com.netflix.client.BufferedResponseCallback;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.http.AsyncBufferingHttpClient;
import com.netflix.client.http.AsyncHttpClient;
import com.netflix.client.http.AsyncHttpClientBuilder;
import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpResponse;
import com.netflix.ribbon.examples.server.ServerResources.Person;
import com.netflix.serialization.HttpSerializationContext;
import com.netflix.serialization.Deserializer;
import com.netflix.serialization.SerializationFactory;
import com.netflix.serialization.Serializer;
import com.netflix.serialization.TypeDef;
import com.thoughtworks.xstream.XStream;

/**
 * An example shows how to customize the {@link AsyncHttpClient} with
 * {@link SerializationFactory} and timeouts
 * 
 * @author awang
 *
 */
public class CustomizedClientExample extends ExampleAppWithLocalResource {
    @Override
    public void run() throws Exception {
        URI uri = new URI(SERVICE_URI + "testAsync/person");
        IClientConfig clientConfig = DefaultClientConfigImpl.getClientConfigWithDefaultValues();
        clientConfig.setProperty(CommonClientConfigKey.ConnectTimeout, "1000");
        clientConfig.setProperty(CommonClientConfigKey.ReadTimeout, "1000");
        HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
        AsyncBufferingHttpClient client = AsyncHttpClientBuilder.withApacheAsyncClient(clientConfig).buildBufferingClient();
        client.addSerializationFactory(new GsonSerializationFactory());
        client.addSerializationFactory(new XStreamFactory());
        try {
            Future<HttpResponse> future = client.execute(request, new BufferedResponseCallback<HttpResponse>() {
                @Override
                public void failed(Throwable e) {
                }

                @Override
                public void completed(HttpResponse response) {
                    try {
                        Person person = response.getEntity(Person.class);
                        System.out.println(person);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        response.close();
                    }
                }

                @Override
                public void cancelled() {
                }
            });
            future.get();
            request = HttpRequest.newBuilder().uri(SERVICE_URI + "testAsync/getXml").build();
            future = client.execute(request, new BufferedResponseCallback<HttpResponse>() {
                @Override
                public void failed(Throwable e) {
                }

                @Override
                public void completed(HttpResponse response) {
                    try {
                        Person person = response.getEntity(Person.class);
                        System.out.println(person);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        response.close();
                    }
                }

                @Override
                public void cancelled() {
                }
            });
            future.get();
        } finally {
            client.close();
        }
    }

    public static void main(String[] args) throws Exception {
        CustomizedClientExample app = new CustomizedClientExample();
        app.runApp();
    }

}

class GsonSerializationFactory implements SerializationFactory<HttpSerializationContext>{

    static final GsonCodec instance = new GsonCodec();
    @Override
    public <T> Deserializer<T> getDeserializer(HttpSerializationContext key, TypeDef<T> type) {
        if (key.getContentType().equalsIgnoreCase("application/json")) {
            return instance;
        }
        return null;
    }

    @Override
    public <T> Serializer<T> getSerializer(HttpSerializationContext key, TypeDef<T> type) {
        if (key.getContentType().equalsIgnoreCase("application/json")) {
            return instance;
        }
        return null;
    }

}

class GsonCodec<T extends Object> implements Serializer<T>, Deserializer<T> {
    static Gson gson = new Gson();

    @Override
    public T deserialize(InputStream in, TypeDef<T> type)
            throws IOException {
        System.out.println("Deserializing using Gson");
        return gson.fromJson(new InputStreamReader(in, "UTF-8"), type.getType());
    }

    @Override
    public void serialize(OutputStream out, T object, TypeDef<?> type) throws IOException {
        gson.toJson(object, new OutputStreamWriter(out, "UTF-8"));
    }
}

class XStreamFactory implements SerializationFactory<HttpSerializationContext> {

    static XmlCodec xml = new XmlCodec();
    @Override
    public <T> Deserializer<T> getDeserializer(
            HttpSerializationContext key, TypeDef<T> type) {
        if (key.getContentType().equalsIgnoreCase("application/xml")) {
            return xml;
        } else {
            return null;
        }
    }

    @Override
    public <T> Serializer<T> getSerializer(HttpSerializationContext key, TypeDef<T> type) {
        if (key.getContentType().equalsIgnoreCase("application/xml")) {
            return xml;
        } else {
            return null;
        }
    }
}

class XmlCodec<T extends Object> implements Serializer<T>, Deserializer<T> {

    XStream xstream = new XStream();

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(InputStream in, TypeDef<T> type)
            throws IOException {
        System.out.println("Deserializing using XStream");
        return (T) xstream.fromXML(in);
    }

    @Override
    public void serialize(OutputStream out, T object, TypeDef<?> type) throws IOException {
        xstream.toXML(object, out);
    }

}

