package kr.co.aiotlab.capstonedesignproject;

public class CardItem {
    private String name, email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CardItem(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public CardItem() {
        this.name = name;
        this.email = email;
    }
}
