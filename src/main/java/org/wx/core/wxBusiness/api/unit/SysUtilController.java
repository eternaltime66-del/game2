package org.wx.core.wxBusiness.api.unit;


import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.ServletInputStream;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.wx.core.wxBase.annotation.NeedHeader;
import org.wx.core.wxBase.annotation.ParamCheck;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxResult;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.unit.HttpRequestUnit;
import org.wx.core.wxBase.unit.HttpServletUnit;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.code.CodeEnum;
import org.wx.core.wxBusiness.common.entity.WxSuperParam;
import org.wx.core.wxBusiness.log.annotation.WxRequestLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * 前端-工具接口
 *
 * @author 无心
 * @since 2021-07-21
 */
@RestController
@RequestMapping("/api/unit")
public class SysUtilController {



    private String filepathWin ="D:\\File\\Work\\Java\\";
    public static String filepathLinux = "/www/wx/file/";

    /**
     * 发送验证码
     * @param account   账号 邮箱
     * @param type      验证码类型
     * @return WxBaseResult
     */
    @PostMapping("/send/code")
    public WxResult<String> codeSend(
            @NotNull @ParamCheck(msg = "邮箱",pattern = "") String account,
            @NotNull @ParamCheck(msg = "类型", enumPattern = CodeEnum.class) CodeEnum type
    ) {
        String phoneCode = "";
        String action = type.toString();
        System.err.println(phoneCode + account + "开始发送验证码");
        String keys = phoneCode + account;
        String key = "send-code-count-" + DateUtil.today() + ">>>" + keys;
        Object obj = Wx.RedisFactory.get(key);
        if (obj == null) {
            Wx.RedisFactory.setBuyDay(key, 1,1);
        } else {
//            int integer = Integer.parseInt(obj.toString());
//            int dayMaxSendNum = 10;
//            ErrorFactory.throwError(integer >= dayMaxSendNum, "20小时内验证码获取次数已达上限");
//            integer += 1;
//            Wx.RedisFactory.setBuyDay(key, integer,1);
        }
        String code = Wx.CodeFactory.sendCode(phoneCode,account, CodeEnum.valueOf(action));
        return WxResult.success();
    }

    /**
     * 上传文件
     * @param file 文件流
     * @return
     * @throws FileNotFoundException
     */
    @RequestMapping("upload")
    public WxResult<Object> upload(
            @NotNull @RequestParam("file") MultipartFile file,
            ServletInputStream stream,
            Boolean key
    ) throws FileNotFoundException {
        key = key!=null && key;
        //System.err.println(file.getOriginalFilename());
        String fileName = key?"1.png":file.getOriginalFilename();
        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowTypes = Arrays.asList("jpg", "png");

        ErrorFactory.throwError(!allowTypes.contains(fileType), "不支持该格式文件上传");
        ErrorFactory.throwError(fileName.chars().filter(ch -> ch == '.').count() > 1, "文件名不合法，包含多重后缀");

        boolean isImg = Arrays.asList("jpg", "png").contains(fileType);

        boolean errorImg = Arrays.asList("jpg", "png").contains(fileType);
        ErrorFactory.throwError(!errorImg,"不支持该格式文件上传");
        String filepath = isWinOs()?filepathWin:filepathLinux;
        String realFilePath = isImg ? (filepath + "images/") : (filepath + "file/");
        File targetFile = new File(realFilePath);
        if (!targetFile.exists()) {
            targetFile.mkdirs();
        }
        String name = WordUnit.nowId(4, 1) + "." + fileType;
        try (FileOutputStream out = new FileOutputStream(realFilePath + name);) {
            if(key){
                byte[] buf= new byte[1024];
                int len ;
                len = stream.read(buf, 0, buf.length);
                while (len!= -1){
                    out.write(buf,0,len);
                }
            }else {
                out.write(file.getBytes());
            }
            HashMap<String,String> map = new HashMap<String,String>();
            String url = String.format("/uploads/%s/%s", (isImg ? "images" : "file"), name);
            map.put("url", url);
            return WxResult.success(map);
        } catch (Exception e) {
            ErrorFactory.throwError("上传异常");

        }
        return WxResult.success();
    }

    public static Boolean isWinOs() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("win");
    }

    /**
     * 获取系统参数
     */
    @PostMapping("/config/list")
    @WxRequestLog
    @NeedHeader(roles = {MemberRole.USER})
    public WxResult<List<WxSuperParam>> levelList(
            WxSuperParam entity
    ){

        IPage<WxSuperParam> iPage = Wx.SuperParamService.pageQuery(entity);
        return WxResult.page(iPage);
    }
}

