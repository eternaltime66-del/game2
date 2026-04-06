package org.wx.core.wxBusiness.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxBaseEntity;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.unit.AesUtil;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.account.entity.enums.MemberKycState;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Member 实体类
 * @author 无心
 * @date 2026-01-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_member")
public class Member extends WxBaseEntity<Member> {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private String id;

    /**
     * 密码
     */
    private String password;

    /**
     * token
     */
    private String token;

    /**
     * 盐
     */
    private String salt;

    /**
     * 用户角色
     */
    private MemberRole memberRole;

    /**
     * 昵称
     */
    private String name;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 备注
     */
    private String remark;

    /**
     * 地址
     */
    private String mnemonic;

    /**
     * 地址
     */
    private String address;



    public void setMnemonicEncrypted(String mnemonicPlain) {
        if (mnemonicPlain == null) {
            this.mnemonic = null;
            return;
        }
        this.mnemonic = encodeMnemonic(mnemonicPlain);
    }

    /**
     * 对助记词进行加密（用于导入钱包时校验）
     * 必须与 setMnemonicEncrypted 保持一致
     */
    public static String encodeMnemonic( String mnemonicPlain) {
        return AesUtil.encrypt(mnemonicPlain, "WXMAX");
    }

    public static Member commonMember(){
        Member member = new Member();
        member.id = WordUnit.randomKey(10, 1);
        member.memberRole =MemberRole.USER;
        member.salt = WordUnit.randomKey(12, 2);
        member.upSort = 0;
        member.inviteCode = WordUnit.randomKey(8,2).toUpperCase();
        return member;
    }

    public void verifyPsd(String password) {
        ErrorFactory.throwError(!this.password.equals(psdEncode(password)),"密码有误");
    }

    public String psdEncode(String password) {
        // 新增安全规则：双盐 + 双层加密
        String step1 = WordUnit.md5(this.id + password + this.salt);
        String s = WordUnit.md5(step1 + this.id);

        return s;
    }

    public static void main(String[] args) {
        String string = WordUnit.md5("9527" + "123456" + "jrwJ");
        System.out.println(WordUnit.md5(string + "9527" ));
    }

    public static String creteToken() {
        return WordUnit.randomKey(12, 2).toUpperCase();
    }

    public void info(){
        this.salt = null;
        this.password = null;
        this.mnemonic = null;
    }

    @Data
    public static class UserMore{
        public MemberKycState kycState;
        public Long ztNum;
        public Long tdNum;
    }

    /**
     * 地址
     */
    @TableField(exist = false)
    private UserMore more = new UserMore();

    @TableField(exist = false)
    private Boolean myFollowUser;

    /**
     * 上级id1
     */
    private String sourceInviteIdL1;

    /**
     * 上级id2
     */
    private String sourceInviteIdL2;

    /**
     * 上级id3
     */
    private String sourceInviteIdL3;

    /**
     * 上级ids
     */
    private String sourceInviteIds;

    /**
     * 层级排序
     */
    private Integer upSort;

    /**
     * 邀请码
     */
    private String inviteCode;

    /**
     * 是否为节点用户
     */
    private Boolean agent;

    /**
     * 邀请码
     */
    private BigDecimal ztRatio;
}
