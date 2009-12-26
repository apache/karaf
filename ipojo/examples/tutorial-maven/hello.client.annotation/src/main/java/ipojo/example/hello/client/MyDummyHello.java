package ipojo.example.hello.client;

import ipojo.example.hello.Hello;

public class MyDummyHello implements Hello {

    public String sayHello(String name) {
        return "Bonjour";
    }

}
