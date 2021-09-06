package org.kk.architecture.servlet;

import com.alibaba.fastjson.JSONObject;
import org.kk.architecture.annotation.*;
import org.kk.architecture.handlermapping.HandlerMapping;
import org.kk.business.bean.Result;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Steven
 */
public class DispatcherServlet extends HttpServlet {

    public final Properties properties = new Properties();

    private final List<String> classNames = new ArrayList<>();

    public  final Map<String, Object> ioc = new ConcurrentHashMap<>();

    private final List<HandlerMapping> handlerMappingsList = new ArrayList<>();

    @Override
    public void init(ServletConfig config)  {

        //1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2、扫描相关的类
        doScanner(properties.getProperty("baseScanPackage"));

        //3、初始化扫描到的类，并且将它们放入到ICO容器之中
        doInstance();

        //4、完成依赖注入
        doAutowired();

        //5、初始化HandlerMapping
        initHandlerMapping();

        System.out.println("spring container init 好了 !!!!");
    }

    private void initHandlerMapping() {

        for (Map.Entry<String, Object> stringObjectEntry : ioc.entrySet()) {
            Object value = stringObjectEntry.getValue();
            String baseUrl = null;
            if (!value.getClass().isAnnotationPresent(Controller.class)) {
                continue;
            }

            if (value.getClass().isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = value.getClass().getAnnotation(RequestMapping.class);
                baseUrl = requestMapping.value();
            }

            Method[] declaredMethods = value.getClass().getDeclaredMethods();

            for (Method declaredMethod : declaredMethods) {

                if (declaredMethod.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping annotation = declaredMethod.getAnnotation(RequestMapping.class);

                    String url;
                    if (baseUrl!=null) {
                        url = baseUrl + annotation.value();
                    } else {
                        url = annotation.value();
                    }
                    handlerMappingsList.add(new HandlerMapping(url, declaredMethod, value));

                }
            }
        }

    }

    private void doAutowired() {
        if ( ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> stringObjectEntry : ioc.entrySet()) {
            Object instance = stringObjectEntry.getValue();

            if (instance.getClass().isAnnotationPresent(Controller.class) ||
                    instance.getClass().isAnnotationPresent(Service.class)) {

                Field[] declaredFields = instance.getClass().getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (declaredField.isAnnotationPresent(Autowired.class)) {
                        declaredField.setAccessible(true);
                        try {
                            String name = declaredField.getType().getName();
                            String beanName = toFirstLowerCase(name);
                            declaredField.set(instance, ioc.get(beanName));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }

    }

    private void doInstance() {

        if ( classNames.isEmpty()) {
            return;
        }

        for (String className : classNames) {
            try {
                Class<?> classInstance = Class.forName(className);
                if (classInstance.isAnnotationPresent(Controller.class)) {

                    String simpleName = classInstance.getSimpleName();
                    String beanName = toFirstLowerCase(simpleName);

                    ioc.put(beanName, classInstance.newInstance());

                } else if (classInstance.isAnnotationPresent(Service.class)) {
                    String simpleName = classInstance.getSimpleName();
                    String beanName = toFirstLowerCase(simpleName);
                    ioc.put(beanName, classInstance.newInstance());
                    Class<?>[] interfaces = classInstance.getInterfaces();
                    //3、根据类型自动赋值,投机取巧的方式
                    for (Class<?> i : interfaces) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The “" + i.getName() + "” is exists!!");
                        }
                        Object instance = classInstance.newInstance();
                        //把接口的类型直接当成key了
                        ioc.put(i.getName(), instance);
                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private String toFirstLowerCase(String simpleName) {
        if (simpleName == null || "".equals(simpleName)) {
            return "";
        }
        String first = simpleName.substring(0, 1);
        return simpleName.replaceFirst(first, first.toLowerCase());
    }

    private void doScanner(String baseScanPackage) {
        URL url = this.getClass().getClassLoader().getResource(baseScanPackage.replaceAll("\\.", "/"));
        if (url==null){return;}
        File classPath = new File(url.getFile());
        File[] files = classPath.listFiles();
        if (files==null){return;}

        for (File file : files) {
            if (file.isDirectory()) {
                doScanner(baseScanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = (baseScanPackage + "." + file.getName().replace(".class", ""));
                classNames.add(className);
            }
        }

    }

    private void doLoadConfig(String initParameter) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(initParameter);
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws  IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws  IOException {

        Map<String, String[]> parameterMap = req.getParameterMap();

        for (Map.Entry<String, String[]> stringEntry : parameterMap.entrySet()) {

            String key = stringEntry.getKey();
            System.out.println("key:"+key);
            String[] value = stringEntry.getValue();
            System.out.println(Arrays.toString(value));
        }



        doDispatcher(req, resp);

    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getRequestURI();

        List<String> collect = handlerMappingsList.stream().map(HandlerMapping::getUrl).collect(Collectors.toList());
        if (!collect.contains(servletPath)) {
            String result = JSONObject.toJSONString(Result.error("not found 404"));
            resp.setHeader("Content-Type", "application/json;charset=utf-8");
            resp.setContentType("application/json");
            resp.setStatus(404);
            resp.getWriter().write(result);
            return;
        }

        for (HandlerMapping handlerMapping : handlerMappingsList) {
            String url = handlerMapping.getUrl();
            if (url.equals(servletPath)) {
                Object controller = handlerMapping.getController();
                Method method = handlerMapping.getMethod();
                method.setAccessible(true);
                try {
                    Class<?> returnType = method.getReturnType();
                    if (returnType.isAssignableFrom(void.class)) {
                        return;
                    }

                    Object invoke = method.invoke(controller, Integer.valueOf(req.getParameter("userId")));
                    if (method.isAnnotationPresent(ResponseBody.class)) {
                        String result = JSONObject.toJSONString(invoke);
                        resp.setHeader("Content-Type", "application/json;charset=utf-8");
                        resp.setContentType("application/json");
                        resp.getWriter().write(result);
                    } else {
                        resp.getWriter().write(invoke.toString());
                    }

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }


}
