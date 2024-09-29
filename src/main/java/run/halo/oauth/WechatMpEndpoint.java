package run.halo.oauth;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.util.WeChatQrCodeCacheUtil;
import run.halo.util.WechatMpUtil;
import run.halo.util.WechatQrCode;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class WechatMpEndpoint implements CustomEndpoint {

    private final String tag = "api.plugin.halo.run/v1alpha1/wechat-mp";
    private final Oauth2LoginConfiguration oauth2LoginConfiguration;
    @Autowired
    private WechatMpUtil wechatMpUtil;
    @Autowired
    private WeChatUserServiceImpl wechatUserService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .nest(RequestPredicates.path("/plugins/plugin-oauth2"), this::nest,
                builder -> builder.operationId("PluginOauthWechatMpEndpoints").description("Plugin OAuth wechat-mp Endpoints").tag(tag)
            )
            .build();
    }

    private RouterFunction<ServerResponse> nest() {
        return SpringdocRouteBuilder.route()
            .GET("/loginQrCode", this::loginQrCode,
                builder -> builder.operationId("LoginQrCode").description("Get the QR code of the WeChat public account").tag(tag)
            )
            .GET("/userLogin",this::userLogin,
                builder -> builder.operationId("UserLogin").description("Check whether the code scanning is complete").tag(tag)
            )
            .GET("/wechat/check",this::wechatCheck,
                builder -> builder.operationId("WechatCheck").description("Verify the WeChat signature").tag(tag)
            )
            .POST("/wechat/checkMsg",this::wechatMsg,
                builder -> builder.operationId("WechatMsg").description("Receive WeChat messages").tag(tag)
            )
            .build();
    }

    Mono<ServerResponse> wechatCheck(ServerRequest request) {
        String signature = request.queryParams().getFirst("signature");
        String timestamp = request.queryParams().getFirst("timestamp");
        String nonce = request.queryParams().getFirst("nonce");
        String echostr = request.queryParams().getFirst("echostr");

        if (StringUtils.isEmpty(signature) || StringUtils.isEmpty(timestamp) || StringUtils.isEmpty(nonce)) {
            return ServerResponse.ok().build();
        }
        wechatUserService.checkSignature(signature, timestamp, nonce);
        return ServerResponse.ok().bodyValue(echostr);
    }

    Mono<ServerResponse> wechatMsg(ServerRequest request) {
        String signature = request.queryParams().getFirst("signature");
        String timestamp = request.queryParams().getFirst("timestamp");
        String nonce = request.queryParams().getFirst("nonce");
        log.info("requestBody:{}", request);
        log.info("signature:{}", signature);
        log.info("timestamp:{}", timestamp);
        log.info("nonce:{}", nonce);
        wechatUserService.checkSignature(signature, timestamp, nonce);
        return ServerResponse.ok().bodyValue(wechatUserService.handleWechatMsg(""));
    }

    /**
     * 校验是否扫描完成
     * 完成，返回 JWT
     * 未完成，返回 check faild
     * @return
     */
    Mono<ServerResponse> userLogin(ServerRequest request) {
        String ticketParams = request.queryParams().getFirst("ticket");
        String openId = WeChatQrCodeCacheUtil.get(ticketParams);
        if (StringUtils.isNotEmpty(openId)) {
            log.info("login success,open id:{}", openId);
            // return ApiResultUtil.success(jwtUtil.createToken(openId));
            return ServerResponse.ok().bodyValue("check success");
        }
        log.info("login error,ticket:{}", ticketParams);
        return ServerResponse.ok().bodyValue("check faild");
    }

    /**
     * 获取验证码
     * @param request
     * @return
     */
    Mono<ServerResponse> loginQrCode(ServerRequest request) {
        WechatQrCode wxCode = wechatMpUtil.getWxQrCode();
        return ServerResponse.ok().bodyValue(wxCode.getQrCodeUrl());
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.plugin.halo.run/v1alpha1");
    }
}
