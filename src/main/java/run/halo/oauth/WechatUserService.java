package run.halo.oauth;

/**
 * @Author zhp
 */
public interface WechatUserService {

    void checkSignature(String signature, String timestamp, String nonce);

    String handleWechatMsg(String requestBody);
}
