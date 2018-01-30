/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fe.api.gateway.sync;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.RegistryService;
import com.google.common.base.Splitter;
import fe.api.gateway.common.Tool;
import fe.api.gateway.protocol.EndPoint;
import fe.core.Assert;
import fe.core.FastJson;
import fe.core.exception.ProtocolException;
import fe.core.exception.SystemException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import javax.swing.text.html.Option;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class RegistryServerSync implements InitializingBean, DisposableBean, NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryServerSync.class);

    private static final URL SUBSCRIBE = new URL("fe-api-gateway", NetUtils.getLocalHost(), 0, "",
            Constants.INTERFACE_KEY, Constants.ANY_VALUE,
            Constants.GROUP_KEY, Constants.ANY_VALUE,
            Constants.VERSION_KEY, Constants.ANY_VALUE,
            Constants.CLASSIFIER_KEY, Constants.ANY_VALUE,
            Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY + ","
            + Constants.CONSUMERS_CATEGORY + ","
            + Constants.ROUTERS_CATEGORY + ","
            + Constants.CONFIGURATORS_CATEGORY,
            Constants.ENABLED_KEY, Constants.ANY_VALUE,
            Constants.CHECK_KEY, String.valueOf(false));

    private static final AtomicLong ID = new AtomicLong();

    /**
     * Make sure ID never changed when the same url notified many times
     */
    private final ConcurrentHashMap<String, Long> URL_IDS_MAPPER = new ConcurrentHashMap<String, Long>();

    // ConcurrentMap<category, ConcurrentMap<servicename, Map<Long, URL>>>
    private final ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> registryCache = new ConcurrentHashMap<String, ConcurrentMap<String, Map<Long, URL>>>();
    @Resource
    private RegistryService registryService;

    public ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> getRegistryCache() {
        return registryCache;
    }

    public void afterPropertiesSet() throws Exception {
        logger.info("Init Dubbo Admin Sync Cache...");
        new Thread(() -> {
            registryService.subscribe(SUBSCRIBE, this);
        }).start();
    }

    public void destroy() throws Exception {
        registryService.unsubscribe(SUBSCRIBE, this);
    }

    // Notification of of any service with any type (override、subcribe、route、provider) is full.
    public void notify(List<URL> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        // Map<category, Map<servicename, Map<Long, URL>>>
        final Map<String, Map<String, Map<Long, URL>>> categories = new HashMap<String, Map<String, Map<Long, URL>>>();
        String interfaceName = null;
        for (URL url : urls) {
            String category = url.getParameter(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
            if (Constants.EMPTY_PROTOCOL.equalsIgnoreCase(url.getProtocol())) { // NOTE: group and version in empty protocol is *
                ConcurrentMap<String, Map<Long, URL>> services = registryCache.get(category);
                if (services != null) {
                    String group = url.getParameter(Constants.GROUP_KEY);
                    String version = url.getParameter(Constants.VERSION_KEY);
                    // NOTE: group and version in empty protocol is *
                    if (!Constants.ANY_VALUE.equals(group) && !Constants.ANY_VALUE.equals(version)) {
                        services.remove(url.getServiceKey());
                    } else {
                        for (Map.Entry<String, Map<Long, URL>> serviceEntry : services.entrySet()) {
                            String service = serviceEntry.getKey();
                            if (Tool.getInterface(service).equals(url.getServiceInterface())
                                    && (Constants.ANY_VALUE.equals(group) || StringUtils.isEquals(group, Tool.getGroup(service)))
                                    && (Constants.ANY_VALUE.equals(version) || StringUtils.isEquals(version, Tool.getVersion(service)))) {
                                services.remove(service);
                            }
                        }
                    }
                }
            } else {
                if (StringUtils.isEmpty(interfaceName)) {
                    interfaceName = url.getServiceInterface();
                }
                Map<String, Map<Long, URL>> services = categories.get(category);
                if (services == null) {
                    services = new HashMap<String, Map<Long, URL>>();
                    categories.put(category, services);
                }
                String service = url.getServiceKey();
                Map<Long, URL> ids = services.get(service);
                if (ids == null) {
                    ids = new HashMap<Long, URL>();
                    services.put(service, ids);
                }

                // Make sure we use the same ID for the same URL
                if (URL_IDS_MAPPER.containsKey(url.toFullString())) {
                    ids.put(URL_IDS_MAPPER.get(url.toFullString()), url);
                } else {
                    long currentId = ID.incrementAndGet();
                    ids.put(currentId, url);
                    URL_IDS_MAPPER.putIfAbsent(url.toFullString(), currentId);
                }
            }
        }
        if (categories.size() == 0) {
            return;
        }
        for (Map.Entry<String, Map<String, Map<Long, URL>>> categoryEntry : categories.entrySet()) {
            String category = categoryEntry.getKey();
            ConcurrentMap<String, Map<Long, URL>> services = registryCache.get(category);
            if (services == null) {
                services = new ConcurrentHashMap<String, Map<Long, URL>>();
                registryCache.put(category, services);
            } else {// Fix map can not be cleared when service is unregistered: when a unique “group/service:version” service is unregistered, but we still have the same services with different version or group, so empty protocols can not be invoked.
                Set<String> keys = new HashSet<String>(services.keySet());
                for (String key : keys) {
                    if (Tool.getInterface(key).equals(interfaceName) && !categoryEntry.getValue().entrySet().contains(key)) {
                        services.remove(key);
                    }
                }
            }
            services.putAll(categoryEntry.getValue());
        }
    }

    public EndPoint buildEndPoint(String methodName,String version) {
        Assert.assertNotNull(methodName,"方法名不能为空!");
        List<String> list = Splitter.on(".").trimResults().omitEmptyStrings().splitToList(methodName);
        if (list.size() != 3)
            throw new ProtocolException("method 格式异常!");

        String application = list.get(0);
        String service = list.get(1);
        String method = list.get(2);

        ConcurrentMap<String, Map<Long, URL>> services = registryCache.get("providers");
        if (MapUtils.isEmpty(services))
            throw new ProtocolException("提供者无法找到!");

        //TODO com.shining3d.zeus.client.DeviceSupplierApiService:zeus_node_1.0.0

        Optional<String> optionalServiceKey = services.keySet()
                .stream()
                .filter(item -> {
                    String s = item.toLowerCase();

                    if (!s.contains(application.toLowerCase()) || !s.contains(service.toLowerCase())) {
                        return false;
                    }
                    if (StringUtils.isEmpty(version)) {
                        return true;
                    }

                    if (s.contains(version)) {
                        return true;
                    } else {
                        return false;
                    }
                }).findFirst();

        if (optionalServiceKey == null || !optionalServiceKey.isPresent())
            throw new ProtocolException("提供者无法找到!");

        Map<Long, URL> map = services.get(optionalServiceKey.get());

        List<URL> urlList = map.values().stream().filter(url -> {
            if (url.getProtocol().equals("jsonrpc")) {
                return true;
            } else {
                return false;
            }
        }).collect(Collectors.toList());

        if(CollectionUtils.isEmpty(urlList))
            throw new ProtocolException("提供者无法找到!");

        URL url = null;
        if (urlList.size() == 1) {
            url = urlList.get(0);
        } else {
            //TODO 此处随机 后续根据提供者负载情况,动态LoadBalance
            int index = RandomUtils.nextInt(0,urlList.size() - 1);
            url = urlList.get(index);
        }

        String ip = url.getIp();
        int port = url.getPort();
        String serviceInterface = url.getServiceInterface();
        return EndPoint.builder().ip(ip)
                .port(port)
                .serviceInterface(serviceInterface).method(method).build();
    }

    public  ConcurrentMap<String, Map<Long, URL>> getProviders() {
        return registryCache.get("providers");
    }





}
    
