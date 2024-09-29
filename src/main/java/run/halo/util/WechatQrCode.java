package run.halo.util;

import lombok.Data;

@Data
public class WechatQrCode {

    private String ticket;
    private Long expireSeconds;
    private String url;
    private String qrCodeUrl;
}
