package dirTraversal.dao;

import java.util.List;

import dirTraversal.model.User;

public interface UserDao {

    public void insertUser (User user);

    public User findUserById (int userId);

    public List<User> findAllUsers();

}
