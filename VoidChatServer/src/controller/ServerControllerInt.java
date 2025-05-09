package controller;

import java.rmi.RemoteException;
import java.util.ArrayList;
import model.ClientModelInt;
import model.Message;
import model.User;
import model.VoiceMessage;
import utilitez.Notification;

public interface ServerControllerInt extends Notification {

  /**
   * save object on hash table in server
   * 
   * @param username
   * @param obj
   * @return false if user already login
   */
  boolean register(String username, ClientModelInt obj);

  /**
   * notify user with status
   * 
   * @param reciver
   * @param message
   * @param type
   */
  @Override
  void notify(String reciver, String message, int type);

  /**
   * if false handel disable send btn
   * 
   * @return
   */
  // boolean sendMsg(String reciver , String msg);
  void recieveMsg(Message message);

  void startServer();

  void stopServer();

  /**
   * 
   * @param username
   * @return User or null if not exsits
   */
  User getUserInfo(String username);

  /**
   * Server Send announcement message to all users
   * 
   * @param message
   */
  void sendAnnouncement(String message);

  void unregister(String username);

  /**
   * make peet-to-peer connection with Client
   * 
   * @param Client
   * @return connection
   */
  ClientModelInt getConnection(String Client);


  /**
   * send image to sponser
   * 
   * @param data
   * @param dataLength
   */
  public void sendSponser(byte[] data, int dataLength);

  /**
   * send email to user
   * 
   * @param to
   * @param subject
   * @param emailBody
   * @return true if success
   */
  boolean sendMail(String to, String subject, String emailBody);

  void updateUser(User user) throws RemoteException;

  void loadErrorServer();

  /**
   * Notify all clients that a user has updated their avatar
   * This allows other clients to refresh the avatar in their UI
   * 
   * @param username The username of the user who updated their avatar
   */
  void notifyAvatarUpdate(String username);

  /**
   * Send a voice message to a user or group
   * 
   * @param voiceMessage The voice message to send
   * @throws RemoteException If there is an error during remote communication
   */
  void sendVoiceMessage(VoiceMessage voiceMessage) throws RemoteException;

}
