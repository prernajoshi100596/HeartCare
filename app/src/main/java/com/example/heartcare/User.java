package com.example.heartcare;

import java.io.Serializable;
@SuppressWarnings("serial")

public class User implements Serializable {
    private double height;
    private double weight;
    private double gender;
    private double age;
    private double cholesterol;
    private double glucose;
    private double smoke;
    private double alco;
    private double active;

    public double getHeight() {
        return height;
    }

    public double getWeight() {
        return weight;
    }

    public double getGender() {
        return gender;
    }

    public double getAge() {
        return age;
    }

    public double getCholesterol() {
        return cholesterol;
    }

    public double getGlucose() {
        return glucose;
    }

    public double getSmoke() {
        return smoke;
    }

    public double getAlco() {
        return alco;
    }

    public double getActive() {
        return active;
    }

    public User(double height, double weight, double gender, double age, double cholesterol, double glucose, double smoke, double alco, double active) {
        this.height = height;
        this.weight = weight;
        this.gender = gender;
        this.age = age;
        this.cholesterol = cholesterol;
        this.glucose = glucose;
        this.smoke = smoke;
        this.alco = alco;
        this.active = active;
    }
}
