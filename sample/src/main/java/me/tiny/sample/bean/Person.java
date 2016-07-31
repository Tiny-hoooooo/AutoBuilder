package me.tiny.sample.bean;

import me.tiny.annotation.AutoBuilder;
import me.tiny.annotation.Ignore;

/**
 * Created by beichen on 16/7/25.
 */
@AutoBuilder
public class Person {
    String name;
    int age;
    Address address;
    @Ignore
    long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}
