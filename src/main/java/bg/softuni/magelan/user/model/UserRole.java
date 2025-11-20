package bg.softuni.magelan.user.model;

import lombok.Getter;

@Getter
public enum UserRole {

     ADMIN("Admin"),
     USER("User");

     private final String displayName;
     UserRole(String displayName) {
         this.displayName = displayName;
     }
}
