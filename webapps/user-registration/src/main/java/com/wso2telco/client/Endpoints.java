/*******************************************************************************
 * Copyright  (c) 2015-2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 * 
 * WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.wso2telco.client;

//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wso2telco.gsma.shorten.SelectShortUrl;
import com.wso2telco.sms.OutboundSMSMessageRequest;
import com.wso2telco.sms.OutboundSMSTextMessage;
import com.wso2telco.sms.SendSMSRequest;
import com.wso2telco.utils.ReadMobileConnectConfig;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.json.JSONException;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.identity.mgt.stub.UserIdentityManagementAdminServiceIdentityMgtServiceExceptionException;
import org.wso2.carbon.identity.user.registration.stub.UserRegistrationAdminServiceException;
import org.wso2.carbon.identity.user.registration.stub.UserRegistrationAdminServiceIdentityException;
import org.wso2.carbon.identity.user.registration.stub.dto.UserDTO;
import org.wso2.carbon.identity.user.registration.stub.dto.UserFieldDTO;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;
import org.wso2.carbon.user.core.UserCoreConstants;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: Auto-generated Javadoc
//import org.json.JSONException;
//import Aloo.AdminServicesInvoker.LoginAdminServiceClient;

 
/**
 * The Class Endpoints.
 */
@Path("/endpoint")
public class Endpoints {

    /** The context. */
    @Context
    private UriInfo context;
    
    /** The log. */
    private static Log log = LogFactory.getLog(Endpoints.class);
    
    /** The user map. */
    private static Map<String, UserRegistrationData> userMap = new HashMap<String, UserRegistrationData>();
   
    /** The success response. */
    String successResponse = "\"" + "amountTransaction" + "\"";
    
    /** The service exception. */
    String serviceException = "\"" + "serviceException" + "\"";
    
    /** The policy exception. */
    String policyException = "\"" + "policyException" + "\"";
    
    /** The error return. */
    String errorReturn = "\"" + "errorreturn" + "\"";

     
    /**
     * Instantiates a new endpoints.
     */
    public Endpoints() {
        
    }


    /**
     * Ussd receive.
     *
     * @param jsonBody the json body
     * @return the response
     * @throws Exception the exception
     */
    @POST
    @Path("/ussd/receive")
    @Consumes("application/json")
    @Produces("application/json")
    public Response ussdReceive(String jsonBody) throws Exception {
        String responseString = null;
        String msisdn = null;

        int responseCode = 201;
        int noOfAttempts = 0;
        
        org.json.JSONObject jsonObj = new org.json.JSONObject(jsonBody);
        
        //Retrive pin and username
        String message = jsonObj.getJSONObject("inboundUSSDMessageRequest").getString("inboundUSSDMessage");
        String sessionID = jsonObj.getJSONObject("inboundUSSDMessageRequest").getString("clientCorrelator");
        
        noOfAttempts = DatabaseUtils.readMultiplePasswordNoOfAttempts(sessionID);
        
        SendUSSD ussdPush = new SendUSSD();
        
        if (!(message.matches("[0-9]+") && message.length() > 3 && message.length() < Integer.parseInt(FileUtil.getApplicationProperty("maxlength") ))){
            
            String notifyUrl = FileUtil.getApplicationProperty("notifyurl");
            
            if( noOfAttempts == 3){
                 //Session Terminated saying user has entered incorrect pin three times.
                    //ussdPush.sendUSSD(msisdn, sessionID, 4,"mtfin");
                responseString = SendUSSD.getJsonPayload(msisdn, sessionID, 6, "mtfin", notifyUrl);
                DatabaseUtils.updateRegStatus(sessionID, "Approved");
                DatabaseUtils.deleteUser(sessionID);
                return Response.status(responseCode).entity(responseString).build();
            }
            responseString = SendUSSD.getJsonPayload(msisdn, sessionID, 4, "mtcont", notifyUrl);
            DatabaseUtils.updateMultiplePasswordNoOfAttempts(sessionID, noOfAttempts +1 );
            
            return Response.status(responseCode).entity(responseString).build();
            
        }
        
    
        //Mobile Number = Username
        msisdn = sessionID ;      
                
        //First Time PIN Retrival
        if(noOfAttempts == 1 || noOfAttempts == 3 || noOfAttempts == 5){
            if(noOfAttempts == 1) {
                DatabaseUtils.deleteRequestType(msisdn);
            }
            //Update with user entered PIN
            DatabaseUtils.updateMultiplePasswordPIN(sessionID, Integer.parseInt(message));
            //Update user attempts
            DatabaseUtils.updateMultiplePasswordNoOfAttempts(sessionID, noOfAttempts + 1);
            String notifyUrl = FileUtil.getApplicationProperty("notifyurl");
            //Send USSD push to user's mobile
            //Ask User to retype password
            //ussdPush.sendUSSD(msisdn, sessionID,2,"mtcont");
            responseString = SendUSSD.getJsonPayload(msisdn, sessionID, 2, "mtcont", notifyUrl);
       }
        
        else if(noOfAttempts == 2 || noOfAttempts == 4 || noOfAttempts == 6 ){
            if(DatabaseUtils.readMultiplePasswordPIN(sessionID) == Integer.parseInt(message)){
                //update PIN in IS user profile and usr is deleted
                addUser(msisdn, message);
                DatabaseUtils.updateRegStatus(sessionID, "Approved");
                
                DatabaseUtils.deleteUser(sessionID);
                
                 //CODE ADDED #PRIYANKA_06608
                LoginAdminServiceClient lAdmin = new LoginAdminServiceClient(FileUtil.getApplicationProperty("isadminurl"));
                String sessionCookie = lAdmin.authenticate(FileUtil.getApplicationProperty("adminusername"), FileUtil.getApplicationProperty("adminpassword"));
                UserIdentityManagementClient identityClient = new UserIdentityManagementClient(FileUtil.getApplicationProperty("isadminurl"), sessionCookie);
                try {
            
                        identityClient.unlockUser(msisdn);
                        //Del.log("unlocked");
                } catch (UserIdentityManagementAdminServiceIdentityMgtServiceExceptionException e) {
                        e.printStackTrace();
						ussdPush.sendUSSD(msisdn, sessionID, 5,"mtfin");
                }
                //CODE ADDED #PRIYANKA_06608
                
            }
            else{
                if(noOfAttempts == 6){
                    //Session Terminated saying user has entered incorrect pin three times.
                    ussdPush.sendUSSD(msisdn, sessionID, 4,"mtfin");
                    DatabaseUtils.deleteUser(sessionID);
                }
                else{
                    String notifyUrl = FileUtil.getApplicationProperty("notifyurl");
                    //Start new PIN session
                    responseString = SendUSSD.getJsonPayload(msisdn, sessionID,3, "mtcont", notifyUrl);
                    //ussdPush.sendUSSD(msisdn, sessionID,1,"mtcont");
                    
                    //Update user attempts
                    DatabaseUtils.updateMultiplePasswordNoOfAttempts(sessionID, noOfAttempts + 1);
                    
                    DatabaseUtils.updateMultiplePasswordPIN(sessionID, 0);
                }
            }
            
        }
        
        else{
            //nop
        }
        
        return Response.status(responseCode).entity(responseString).build();
    }



    
        
    /**
     * User pin.
     *
     * @param userName the user name
     * @param msisdn the msisdn
     * @param openId the open id
     * @param pwd the pwd
     * @param claim the claim
     * @param domain the domain
     * @param params the params
     * @param jsonBody the json body
     * @return the response
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @GET
    @Path("/ussd/pin")
   // @Consumes("application/json")
    @Produces("application/json")
    public Response userPIN(@QueryParam("username") String userName, @QueryParam("msisdn") String msisdn,
                            @QueryParam("openId") String openId, @QueryParam("password") String pwd,
                            @QueryParam("claim") String claim, @QueryParam("domain") String domain,
                            @QueryParam("params") String params, String jsonBody) throws IOException, SQLException   {

        UserRegistrationData userRegistrationData = new UserRegistrationData(userName, msisdn, openId, pwd, claim, domain, params);
        userMap.put(msisdn, userRegistrationData);
 
        String responseString = null;
        
        SendUSSD ussdPush = new SendUSSD();
        
        //Send USSD push to user's mobile
        System.out.println("MSISDN =" + msisdn);
        System.out.println("Username =" + userName);
        
        ussdPush.sendUSSD(msisdn, userName,1,"mtinit");

        if(DatabaseUtils.isExistingUser(userName)){
            DatabaseUtils.deleteUser(userName);
        }

        DatabaseUtils.insertMultiplePasswordPIN(userName);

        if(DatabaseUtils.isExistingUserStatus(userName)){
            DatabaseUtils.deleteUserStatus(userName);
        }
        
        DatabaseUtils.insertUserStatus(userName, "pending");
       
        
        return Response.status(200).entity(responseString).build();
    }
    
    
    /**
     * User status.
     *
     * @param username the username
     * @param jsonBody the json body
     * @return the response
     * @throws SQLException the SQL exception
     */
    @GET
    @Path("/ussd/status")
   // @Consumes("application/json")
    @Produces("application/json")
    public Response userStatus(@QueryParam("username") String username, String jsonBody) throws SQLException {
        
        String userStatus = null;
        String responseString = null;
        
        userStatus = DatabaseUtils.getUSerStatus(username); 
		
	if (userStatus.equals("Approved")){
        
            DatabaseUtils.deleteUserStatus(username);
        }
        
        responseString = "{" + "\"username\":\"" + username + "\","
                    + "\"status\":\"" + userStatus + "\"" + "}";
        
               
        return Response.status(200).entity(responseString).build();
    }
    
    
    /**
     * Send sms.
     *
     * @param userName the user name
     * @param msisdn the msisdn
     * @param openId the open id
     * @param pwd the pwd
     * @param claim the claim
     * @param domain the domain
     * @param params the params
     * @param jsonBody the json body
     * @return the response
     * @throws SQLException the SQL exception
     * @throws RemoteException the remote exception
     * @throws Exception the exception
     */
    @GET
    @Path("/sms/send")
   // @Consumes("application/json")
    @Produces("application/json")
    public Response sendSMS(@QueryParam("username") String userName, @QueryParam("msisdn") String msisdn,
                            @QueryParam("openId") String openId, @QueryParam("password") String pwd,
                            @QueryParam("claim") String claim, @QueryParam("domain") String domain,
                            @QueryParam("params") String params, String jsonBody) throws SQLException, RemoteException, Exception {

        UserRegistrationData userRegistrationData = new UserRegistrationData(userName, msisdn, openId, pwd, claim, domain, params);
        userMap.put(msisdn, userRegistrationData);

        ReadMobileConnectConfig readMobileConnectConfig = new ReadMobileConnectConfig();
        Map<String, String> readMobileConnectConfigResult;
        readMobileConnectConfigResult = readMobileConnectConfig.query("dialog/SMS");
        String messageText = readMobileConnectConfigResult.get("MessageContent");

        List<String> destinationAddresses = new ArrayList<String>();
        destinationAddresses.add("tel:" + msisdn);

        if(DatabaseUtils.isExistingUserStatus(userName)){
            DatabaseUtils.deleteUserStatus(userName);
        }

        String uuid=DatabaseUtils.insertUserStatus(userName, "pending");
        
        //String message = "Please click following link to complete the Registration " + FileUtil.getApplicationProperty("callbackurl") + "?" +"msisdn=" + msisdn;
        String message= "Please click following link to complete the Registration " ;
        String sendUrl=FileUtil.getApplicationProperty("callbackurl") + "?" +"id=" + uuid;
        if(readMobileConnectConfigResult.get("IsShortUrl").equalsIgnoreCase("true")){
            SelectShortUrl selectShortUrl=new SelectShortUrl();
            sendUrl=selectShortUrl.getShortUrl(readMobileConnectConfigResult.get("ShortUrlClass"),message,readMobileConnectConfigResult.get("AccessToken"),readMobileConnectConfigResult.get("ShortUrlService"));
            message="Please click following link to complete the Registration "+sendUrl;
            }
        else {
            message = message + sendUrl;
        }


        System.out.println(message);
        String password = FileUtil.getApplicationProperty("password");
        String applicationId = FileUtil.getApplicationProperty("applicationId");
        SendSMS sms = new SendSMS();
        
        sms.setAddress(destinationAddresses);
        sms.setMessage(message);
        sms.setPassword(password);
        sms.setApplicationId(applicationId);
        
        Gson gson = new GsonBuilder().serializeNulls().create();
        
        String returnString = gson.toJson(sms);

        try {
            postRequest(FileUtil.getApplicationProperty("smsendpoint"),returnString);
        } catch (IOException ex) {
            Logger.getLogger(Endpoints.class.getName()).log(Level.SEVERE, null, ex);
        }
        

               
        return Response.status(200).entity(returnString).build();
    }


    /**
     * Send sms one api.
     *
     * @param userName the user name
     * @param msisdn the msisdn
     * @param openId the open id
     * @param pwd the pwd
     * @param claim the claim
     * @param domain the domain
     * @param params the params
     * @param jsonBody the json body
     * @return the response
     * @throws SQLException the SQL exception
     * @throws RemoteException the remote exception
     * @throws Exception the exception
     */
    @GET
    @Path("/sms/oneapi")
    // @Consumes("application/json")
    @Produces("application/json")
    public Response sendSMSOneAPI(@QueryParam("username") String userName, @QueryParam("msisdn") String msisdn,
                                  @QueryParam("openId") String openId, @QueryParam("password") String pwd,
                                  @QueryParam("claim") String claim, @QueryParam("domain") String domain,
                                  @QueryParam("params") String params, String jsonBody) throws SQLException, RemoteException, Exception {

        UserRegistrationData userRegistrationData = new UserRegistrationData(userName, msisdn, openId, pwd, claim, domain, params);
        userMap.put(msisdn, userRegistrationData);

        List<String> address = new ArrayList<String>();
        address.add("tel:+" + msisdn);

        String message = "Please click following link to complete the Registration " + FileUtil.getApplicationProperty("callbackurl") + "?" + "msisdn=" + msisdn;


        OutboundSMSTextMessage messageObj = new OutboundSMSTextMessage();
        messageObj.setMessage(message);

        OutboundSMSMessageRequest outbound = new OutboundSMSMessageRequest();

        outbound.setOutboundTextMessage(messageObj);
        outbound.setAddress(address);
        outbound.setSenderAddress("26451");

        SendSMSRequest req = new SendSMSRequest();

        req.setOutboundSMSMessageRequest(outbound);

        Gson gson = new GsonBuilder().serializeNulls().create();

        String returnString = gson.toJson(req);

        try {
            postRequest(FileUtil.getApplicationProperty("smsendpoint"), returnString);
        } catch (IOException ex) {
            Logger.getLogger(Endpoints.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (DatabaseUtils.isExistingUserStatus(userName)) {
            DatabaseUtils.deleteUserStatus(userName);
        }

        DatabaseUtils.insertUserStatus(userName, "pending");
        return Response.status(200).entity(returnString).build();
    }


    /**
     * Sms confirm.
     *
     * @param id the id
     * @return the response
     * @throws SQLException the SQL exception
     * @throws UserRegistrationAdminServiceIdentityException the user registration admin service identity exception
     * @throws RemoteException the remote exception
     * @throws UserRegistrationAdminServiceException the user registration admin service exception
     */
    @GET
    @Path("/sms/response")
   // @Consumes("application/json")
    @Produces("text/plain")
    //public Response smsConfirm(@QueryParam("msisdn") String msisdn) throws SQLException, UserRegistrationAdminServiceIdentityException, RemoteException, UserRegistrationAdminServiceException {
   public Response smsConfirm(@QueryParam("id") String id) throws SQLException, UserRegistrationAdminServiceIdentityException, RemoteException, UserRegistrationAdminServiceException {
        String msisdn=DatabaseUtils.getUserNameById(id);
        UserRegistrationData userRegistrationData = userMap.get(msisdn);
        String responseString = null;
        responseString = "Your registration confirm time has elapsed. Please register again";
        long waitingTime = Integer.parseInt(FileUtil.getApplicationProperty("waitinTimeInMinutes")) * 1000 * 60;

        long currentTime = System.currentTimeMillis();
        if ((userRegistrationData != null) && (currentTime - userRegistrationData.getUserRegistrationTime()) <= waitingTime) {
            String adminURL = FileUtil.getApplicationProperty("isadminurl") + "/services/UserRegistrationAdminService";
            UserRegistrationAdminServiceClient userRegistrationAdminServiceClient = new UserRegistrationAdminServiceClient(adminURL);
            UserFieldDTO[] userFieldDTOs = userRegistrationAdminServiceClient.readUserFieldsForUserRegistration(userRegistrationData.getClaim());

            String[] fieldValues = userRegistrationData.getFieldValues().split(",");
            for (int count = 0; count < fieldValues.length; count++) {
                userFieldDTOs[count].setFieldValue(fieldValues[count]);
            }

            UserDTO userDTO = new UserDTO();
            userDTO.setOpenID(userRegistrationData.getOpenId());
            userDTO.setPassword(userRegistrationData.getPassword());
            userDTO.setUserFields(userFieldDTOs);
            userDTO.setUserName(userRegistrationData.getUserName());

            userRegistrationAdminServiceClient.addUser(userDTO);

            DatabaseUtils.updateRegStatus(msisdn, "Approved");
            DatabaseUtils.updateAuthenticateData(msisdn,"1");
            //removing user from the static Map , after registering user properly
            userMap.remove(msisdn);

            responseString = "Welcome to Mobile Connect !, simple and secure login solution with strong privacy protection";
        }

        return Response.status(200).entity(responseString).build();
    }

    
    /**
     * User hash.
     *
     * @param answer1 the answer1
     * @return the response
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws NoSuchAlgorithmException the no such algorithm exception
     */
    @GET
    @Path("/ussd/hash")
   // @Consumes("application/json")
    @Produces("application/json")
    public Response userHash(@QueryParam("answer") String answer1) throws IOException, NoSuchAlgorithmException  {
        String responseString = null;
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(answer1.getBytes("UTF-8"));
        
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        System.out.println("UserHash");
        
        String hashString = hexString.toString();
        
        responseString = "{" + "\"hash\":\"" + hashString + "\"" + "}";
               
        return Response.status(200).entity(responseString).build();
    }
    
    /**
     * Inits the ussd.
     *
     * @param msisdn the msisdn
     * @return the response
     */
    @GET
    @Path("/ussd/init")
    @Produces("application/json")
    public Response initUSSD(@QueryParam("msisdn") String msisdn) {
        //new SendUSSD().sendUSSD(msisdn, sessionID, 4,"mtfin");
        String responseString = null;
        try {
            Integer requestType = DatabaseUtils.getPendingUSSDRequestType(msisdn);
            
            if(requestType == 1 || requestType == 3) {//Register or PIN reset
                String notifyUrl = FileUtil.getApplicationProperty("notifyurl");
                responseString = SendUSSD.getJsonPayload(msisdn, msisdn, 1, "mtcont", notifyUrl);//notify url ->MediationTest
            } else if(requestType == 2) {//User Login
                String notifyUrl = FileUtil.getApplicationProperty("loginNotifyurl");
                String ussdMessage = FileUtil.getApplicationProperty("loginmessage");
                responseString = SendUSSD.getJsonPayload(msisdn, msisdn, "mtcont", notifyUrl, ussdMessage);//notify url ->MavenProj
            }
            if(DatabaseUtils.isExistingUser(msisdn)){
                DatabaseUtils.deleteUser(msisdn);
            }
            DatabaseUtils.insertMultiplePasswordPIN(msisdn);//set numer of attempt = 1
        } catch (Exception ex) {
            Logger.getLogger(Endpoints.class.getName()).log(Level.SEVERE, null, ex);
//            responseString = "{\"status\":\"-1\",\"message\":\"error\"}";
            responseString = "{\"error\":\"404\"}";
        }
        //String responseString = "{\"status\":\"success\",\"message\":\"\"PIN reset request sent to mobile phone \" + msisdn + \".\"}";
        return Response.status(200).entity(responseString).build();
    }
    
    /**
     * Save request type.
     *
     * @param msisdn the msisdn
     * @param requestType the request type
     * @return the response
     */
    @GET
    @Path("/ussd/saverequest")
    @Produces("application/json")
    public Response saveRequestType(@QueryParam("msisdn") String msisdn, @QueryParam("requesttype") Integer requestType) {

        String responseString = null;
        int status = -1;
        String message = "error";
        try {
            status = DatabaseUtils.saveRequestType(msisdn, requestType);
            message = "success";
        } catch (Exception ex) {
            Logger.getLogger(Endpoints.class.getName()).log(Level.SEVERE, null, ex);
        }
        responseString = "{\"status\":\"" + status + "\",\"message\":\"" + message + "\"}";
        return Response.status(200).entity(responseString).build();
    }
    
    /**
     * Gets the hash value.
     *
     * @param value the value
     * @return the hash value
     * @throws NoSuchAlgorithmException the no such algorithm exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    private static String getHashValue(String value) throws NoSuchAlgorithmException, UnsupportedEncodingException{

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes("UTF-8"));
        
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        System.out.println("UserHash");
        
        String hashString = hexString.toString();
        
        
        return hashString;
    }

    /**
     * Adds the user.
     *
     * @param userName the user name
     * @param pin the pin
     * @throws Exception the exception
     */
    private void addUser(String userName, String pin) throws Exception {
        UserRegistrationData userRegistrationData = userMap.get(userName);
        if (isUserExists(userName)) {
            updatePIN(userName, pin);
        } else {
            long waitingTime = Integer.parseInt(FileUtil.getApplicationProperty("waitinTimeInMinutes")) * 1000 * 60;

            long currentTime = System.currentTimeMillis();
            if ((userRegistrationData != null) && (currentTime - userRegistrationData.getUserRegistrationTime()) <= waitingTime) {
                String adminURL = FileUtil.getApplicationProperty("isadminurl") + "/services/UserRegistrationAdminService";
                UserRegistrationAdminServiceClient userRegistrationAdminServiceClient = new UserRegistrationAdminServiceClient(adminURL);
                UserFieldDTO[] userFieldDTOs = userRegistrationAdminServiceClient.readUserFieldsForUserRegistration(userRegistrationData.getClaim());
                pin = getHashValue(pin);

                String[] fieldValues = userRegistrationData.getFieldValues().split(",");
                for (int count = 0; count < fieldValues.length; count++) {
                    userFieldDTOs[count].setFieldValue(fieldValues[count]);
                    if (userFieldDTOs[count].getFieldName().equals("pin")) {
                        userFieldDTOs[count].setFieldValue(pin);
                    }
                }

                UserDTO userDTO = new UserDTO();
                userDTO.setOpenID(userRegistrationData.getOpenId());
                userDTO.setPassword(userRegistrationData.getPassword());
                userDTO.setUserFields(userFieldDTOs);
                userDTO.setUserName(userRegistrationData.getUserName());

                userRegistrationAdminServiceClient.addUser(userDTO);

                DatabaseUtils.updateRegStatus(userName, "Approved");
                DatabaseUtils.updateAuthenticateData(userName,"1");
            }
        }
        //removing user from the static Map , after registering user properly
        userMap.remove(userName);
    }

    /**
     * Update pin.
     *
     * @param userName the user name
     * @param pin the pin
     * @throws NoSuchAlgorithmException the no such algorithm exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws AxisFault the axis fault
     * @throws RemoteException the remote exception
     * @throws LoginAuthenticationExceptionException the login authentication exception exception
     */
    private static void updatePIN(String userName, String pin) throws NoSuchAlgorithmException, UnsupportedEncodingException, AxisFault, RemoteException, LoginAuthenticationExceptionException {

        LoginAdminServiceClient lAdmin = new LoginAdminServiceClient(FileUtil.getApplicationProperty("isadminurl"));
        String sessionCookie = lAdmin.authenticate(FileUtil.getApplicationProperty("adminusername"), FileUtil.getApplicationProperty("adminpassword"));

        RemoteUserStoreServiceAdminClient remoteUserStoreServiceAdminClient =
                new RemoteUserStoreServiceAdminClient(FileUtil.getApplicationProperty("isadminurl"), sessionCookie);

        //Hashing user PIN
        pin = getHashValue(pin);
        try {
            //User claim update
            remoteUserStoreServiceAdminClient.setUserClaim(userName, "http://wso2.org/claims/pin", pin, UserCoreConstants.DEFAULT_PROFILE);
        } catch (RemoteException ex) {
            Logger.getLogger(Endpoints.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RemoteUserStoreManagerServiceUserStoreExceptionException ex) {
            Logger.getLogger(Endpoints.class.getName()).log(Level.SEVERE, null, ex);
        }

    }


    
    /**
     * Post request.
     *
     * @param url the url
     * @param requestStr the request str
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void postRequest(String url, String requestStr) throws IOException {

        HttpClient client = new DefaultHttpClient();
        HttpPost postRequest = new HttpPost(url);
        
        postRequest.addHeader("accept", "application/json");
        postRequest.addHeader("Authorization", "Bearer " + FileUtil.getApplicationProperty("accesstoken"));
       

        StringEntity input = new StringEntity(requestStr);
        input.setContentType("application/json");
        
        postRequest.setEntity(input);

        HttpResponse response = client.execute(postRequest);

        if ( (response.getStatusLine().getStatusCode() != 201)){
            //LOG.info("Error occurred while calling end points");
        }
        else{
           // LOG.info("Success Request");
        }
        
    }
    
    /**
     * Ussd pin resend.
     *
     * @param jsonBody the json body
     * @return the response
     * @throws SQLException the SQL exception
     * @throws JSONException the JSON exception
     * @throws JSONException the JSON exception
     */
    @POST
    @Path("/ussd/pin/resend")
    @Consumes("application/json")
    @Produces("application/json")    
    public Response ussdPinResend(String jsonBody) throws SQLException, org.codehaus.jettison.json.JSONException, JSONException {
        Gson gson = new GsonBuilder().serializeNulls().create();
        org.json.JSONObject jsonObj = new org.json.JSONObject(jsonBody);
        String sessionID = jsonObj.getString("sessionID");
        String msisdn = null;
        String msisdnStr = jsonObj.getString("msisdn");;
        if(msisdnStr != null) {//tel:+tel:+94773333428
            msisdn=msisdnStr.split(":\\+")[2];
        }
        int noOfAttempts=1;
        String action="mtcont";
        String status = "PENDING";
        
        SendUSSD sendUSSD=new SendUSSD();
        try {
            sendUSSD.sendUSSDLogin(msisdn, sessionID, noOfAttempts, action);
            DatabaseUtils.updateStatus(sessionID, status);
        } catch (IOException ex) {
            Logger.getLogger(Endpoints.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Response.status(200).entity("{message:SUCCESS}").build();
    }


    /**
     * Send sms one api.
     *
     * @param userName the user name
     * @param jsonBody the json body
     * @return the response
     * @throws SQLException the SQL exception
     * @throws RemoteException the remote exception
     * @throws Exception the exception
     */
    @GET
    @Path("/user/exists")
    // @Consumes("application/json")
    @Produces("application/json")
    public Response sendSMSOneAPI(@QueryParam("username") String userName, String jsonBody) throws SQLException, RemoteException, Exception {

        String returnString = String.valueOf(isUserExists(userName));
        return Response.status(200).entity(returnString).build();
    }

    /**
     * Checks if is user exists.
     *
     * @param userName the user name
     * @return true, if is user exists
     * @throws SQLException the SQL exception
     * @throws RemoteException the remote exception
     * @throws Exception the exception
     */
    private boolean isUserExists(String userName) throws SQLException, RemoteException, Exception {
        LoginAdminServiceClient lAdmin = new LoginAdminServiceClient(FileUtil.getApplicationProperty("isadminurl"));
        String sessionCookie = lAdmin.authenticate(FileUtil.getApplicationProperty("adminusername"), FileUtil.getApplicationProperty("adminpassword"));

        RemoteUserStoreServiceAdminClient remoteUserStoreServiceAdminClient =
                new RemoteUserStoreServiceAdminClient(FileUtil.getApplicationProperty("isadminurl"), sessionCookie);
        boolean userExists = false;
        if (remoteUserStoreServiceAdminClient.isExistingUser(userName)) {
            userExists = true;   //user already exists
        }
        return userExists;
    }

    /**
     * Sets the user claim value.
     *
     * @param msisdn the msisdn
     * @param claimValue the claim value
     * @return the response
     * @throws SQLException the SQL exception
     * @throws JSONException the JSON exception
     * @throws JSONException the JSON exception
     * @throws RemoteException the remote exception
     * @throws LoginAuthenticationExceptionException the login authentication exception exception
     * @throws RemoteUserStoreManagerServiceUserStoreExceptionException the remote user store manager service user store exception exception
     */
    @GET
    @Path("/user/setclaim")
    @Produces("application/json")
    public Response setUserClaimValue(@QueryParam("msisdn") String msisdn,@QueryParam("claimValue") String claimValue) throws SQLException, org.codehaus.jettison.json.JSONException, JSONException, RemoteException, LoginAuthenticationExceptionException, RemoteUserStoreManagerServiceUserStoreExceptionException {

        LoginAdminServiceClient lAdmin = new LoginAdminServiceClient(FileUtil.getApplicationProperty("isadminurl"));
        String sessionCookie = lAdmin.authenticate(FileUtil.getApplicationProperty("adminusername"), FileUtil.getApplicationProperty("adminpassword"));

        RemoteUserStoreServiceAdminClient remoteUserStoreServiceAdminClient=new RemoteUserStoreServiceAdminClient(FileUtil.getApplicationProperty("isadminurl"), sessionCookie);
        remoteUserStoreServiceAdminClient.setUserClaim(msisdn,"http://wso2.org/claims/authenticator",claimValue,msisdn);
        return Response.status(200).entity("{message:SUCCESS}").build();

    }




    /**
     * User authenticator.
     *
     * @param userName the user name
     * @param msisdn the msisdn
     * @param openId the open id
     * @param pwd the pwd
     * @param claim the claim
     * @param domain the domain
     * @param params the params
     * @param authenticator the authenticator
     * @param jsonBody the json body
     * @return the response
     * @throws Exception the exception
     */
    @GET
    @Path("/user/authenticator")
    @Produces("application/json")
    public Response userAuthenticator(@QueryParam("username") String userName, @QueryParam("msisdn") String msisdn,
                            @QueryParam("openId") String openId, @QueryParam("password") String pwd,
                            @QueryParam("claim") String claim, @QueryParam("domain") String domain,
                            @QueryParam("params") String params,@QueryParam("authenticator") String authenticator, String jsonBody) throws Exception {

        UserRegistrationData userRegistrationData = new UserRegistrationData(userName, msisdn, openId, pwd, claim, domain, params);
        userMap.put(msisdn, userRegistrationData);

        String responseString = null;

        if(DatabaseUtils.isExistingUser(userName)){
            DatabaseUtils.deleteUser(userName);
        }

        DatabaseUtils.insertMultiplePasswordPIN(userName);

        if(DatabaseUtils.isExistingUserStatus(userName)){
            DatabaseUtils.deleteUserStatus(userName);
        }
        addUser(msisdn, pwd);
        DatabaseUtils.insertUserStatus(userName, "Approved");

        return Response.status(200).entity(responseString).build();


    }

    /**
     * Send authatication.
     *
     * @param scope the scope
     * @param redirectUri the redirect uri
     * @param clientId the client id
     * @param responseType the response type
     * @param acrValue the acr value
     * @param jsonBody the json body
     * @return the response
     * @throws SQLException the SQL exception
     * @throws RemoteException the remote exception
     * @throws Exception the exception
     */
    //add data for in-line authentication
    @GET
    @Path("/user/authenticate/add")
    @Produces("application/json")
    public Response sendAuthatication(@QueryParam("scope") String scope,@QueryParam("redirecturi")
               String redirectUri,@QueryParam("clientid") String clientId,
                                      @QueryParam("responsetype") String responseType,@QueryParam("acrvalue") String acrValue,String jsonBody) throws SQLException, RemoteException, Exception {
        UUID tokenId = UUID.randomUUID();
        AuthenticationData authenticationData=new AuthenticationData();
        authenticationData.setScope(scope);
        authenticationData.setTokenID(tokenId.toString());
        authenticationData.setClientId(clientId);
        authenticationData.setRedirectUri(redirectUri);
        authenticationData.setResponseType(responseType);
        authenticationData.setAcrValues(Integer.parseInt(acrValue));
        DatabaseUtils.saveAuthenticateData(authenticationData);
        String responseString = tokenId.toString();
        return Response.status(200).entity(responseString).build();
    }


    /**
     * Gets the authatication.
     *
     * @param tokenid the tokenid
     * @param jsonBody the json body
     * @return the authatication
     * @throws SQLException the SQL exception
     * @throws RemoteException the remote exception
     * @throws Exception the exception
     */
    //get data for in-line authentication
    @GET
    @Path("/user/authenticate/get")
    @Produces("application/json")
    public Response getAuthatication(@QueryParam("tokenid") String tokenid,String jsonBody) throws SQLException, RemoteException, Exception {
        AuthenticationData authenticationData= DatabaseUtils.getAuthenticateData(tokenid);
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(authenticationData);
        return Response.status(200).entity(json.toString()).build();
    }

    /**
     * Update authatication msisdn.
     *
     * @param tokenid the tokenid
     * @param msisdn the msisdn
     * @param jsonBody the json body
     * @return the response
     * @throws SQLException the SQL exception
     * @throws RemoteException the remote exception
     * @throws Exception the exception
     */
    @GET
    @Path("/user/authenticate/updatemsisdn")
    @Produces("application/json")
    public Response updateAuthaticationMsisdn(@QueryParam("tokenid") String tokenid,@QueryParam("msisdn") String msisdn,String jsonBody) throws SQLException, RemoteException, Exception {
        AuthenticationData authenticationData= DatabaseUtils.getAuthenticateData(tokenid);
        DatabaseUtils.updateAuthenticateDataMsisdn(tokenid,msisdn);
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(authenticationData);
        return Response.status(200).entity(json.toString()).build();
    }


}