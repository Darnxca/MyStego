package com.example.mystego.firebase.model.User;

import java.util.Objects;

/**
 * Bean del'utente su firebase
 */
public class User {

    private String name;
    private final String email;
    private String idUsr;

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public User(String idUsr, String name, String email) {
        this.idUsr = idUsr;
        this.name = name;
        this.email = email;
    }

    public String getIdUsr() {
        return idUsr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return name.equals(user.name) && email.equals(user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email);
    }

}
