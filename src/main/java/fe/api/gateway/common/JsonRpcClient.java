package fe.api.gateway.common;

import fe.api.gateway.domain.JsonError;
import fe.api.gateway.domain.JsonRpcSuccess;
import fe.api.gateway.protocol.EndPoint;
import fe.core.ApiResponse;
import fe.core.Constants;
import fe.core.FastJson;
import fe.core.HttpClientUtils;
import fe.core.exception.ProtocolException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Component;

/**
 * Created by fe on 2018/1/30.
 */
@Slf4j
@Component
public class JsonRpcClient {

    public ApiResponse execute(EndPoint endPoint) {
        String url = "http://" + endPoint.getIp() + ":" + endPoint.getPort() + "/" + endPoint.getServiceInterface();
        String json = "{\"id\":" + randomId() + "," + "\"method\":\"" + endPoint.getMethod() + "\",\"params\":" + endPoint.getParams() + "}";
        log.info("req : url : {}, body : {}",new Object[]{url,json});
        String result = HttpClientUtils.postRequestBody(url, json);
        log.info("res : {}",result);

        if (result.contains("jsonrpc") && result.contains("id") && result.contains("code") && result.contains("message")) {
           JsonError jsonError = FastJson.fromJson(result,JsonError.class);
           return ApiResponse.errorApiResponse(Constants.PROTOCOL_ERROR,jsonError.getError().getMessage());
        }

        if (result.contains("jsonrpc") && result.contains("id") && result.contains("result")) {
            JsonRpcSuccess jsonRpcSuccess = FastJson.fromJson(result,JsonRpcSuccess.class);
            return ApiResponse.successApiResponse(jsonRpcSuccess.getResult().getResult());
        }
        throw new ProtocolException("服务异常!");
    }

    private long randomId() {
        return RandomUtils.nextLong(1,999999);
    }

}
