package fe.api.gateway.controller;

import com.google.common.collect.Maps;
import fe.api.gateway.common.JsonRpcClient;
import fe.api.gateway.protocol.EndPoint;
import fe.api.gateway.protocol.Req;
import fe.api.gateway.sync.RegistryServerSync;
import fe.core.ApiResponse;
import fe.core.Assert;
import fe.core.FastJson;
import fe.core.HttpClientUtils;
import fe.core.exception.BizException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by fe on 2018/1/26.
 */
@RestController
public class ApiController {
    @Resource
    private RegistryServerSync registryServerSync;

    @Resource
    private JsonRpcClient jsonRpcClient;

    @RequestMapping("/api")
    public ApiResponse execute(Req req) {
        Assert.assertNotNull(req.getMethod(),"method不能为空!");
        EndPoint endPoint = registryServerSync.buildEndPoint(req.getMethod(),req.getVersion());
        return jsonRpcClient.execute(endPoint);
    }

    @RequestMapping("/echoProviders")
    public ApiResponse echoProviders() {
        return ApiResponse.successApiResponse().withData(registryServerSync.getProviders());
    }

    private long randomId() {
        return RandomUtils.nextLong(1,999999);
    }
}
