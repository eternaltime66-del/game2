package org.wx.core.wxBusiness.account.service;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.web3unit.CryptoUtils;
import org.wx.core.web3unit.Web3HashCheckResult;
import org.wx.core.web3unit.Web3Tool;
import org.wx.core.wxBase.annotation.RedisLock;
import org.wx.core.wxBase.base.Wx;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.unit.ListUnit;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.account.entity.Web3Coin;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.account.entity.enums.MoneyDirectionType;
import org.wx.core.wxBusiness.account.entity.enums.MoneyRecordType;
import org.wx.core.wxBusiness.account.entity.enums.PointCoin;
import org.wx.core.wxBusiness.account.mapper.MemberMapper;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.code.CodeEnum;
import org.wx.core.wxBusiness.game.service.GameHeroService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Member Service实现类
 *
 * @author 无心
 * @date 2026-01-16
 */
@Service
public class MemberService extends WxServiceImpl<MemberMapper, Member> {

    @Resource
    private GameHeroService gameHeroService;

    @Transactional(rollbackFor = Exception.class)
    public void initUser(String uid) {
        Member member = new Member();
        Member oldMember = this.getById(uid);
        if (oldMember == null) {
            member.setId(uid);
            String mnemonic = Wx.Web3WalletService.initWeb3Wallet(uid);
            member.setMnemonicEncrypted(mnemonic);
            member.setMemberRole(MemberRole.USER);
            this.save(member);
            Wx.PointWalletService.getSysPointWallet(uid, PointCoin.USDT);
        }
    }

    /**
     * 注册用户 公共方法
     */
    @Transactional(rollbackFor = Exception.class)
    public Member addUser(
            String address,
            String phone,
            String phoneCode,
            String email,
            String psd,
            String psdAgain
    ) {
        Member member = Member.commonMember();
        boolean isEmail = Wx.notEmpty(email);
//        boolean isPhone = Wx.notEmpty(phone) && Wx.notEmpty(phoneCode);
//        boolean isAddress = Wx.notEmpty(address);
        boolean isEmptyPsd = Wx.isEmpty(psd) || Wx.isEmpty(psdAgain);
        if (isEmptyPsd) {
            psd = "123456";
            psdAgain = "123456";
        }
        ErrorFactory.notEquals(psd, psdAgain, "两次密码不一致");
        member.setEmail(email);
        member.setPassword(member.psdEncode(psd));
        member.setToken(Member.creteToken());
        member.setAddress(address);
        this.save(member);
        Wx.RedisFactory.setBuyDay(member.getToken(), member.getId(), 7);
        gameHeroService.initHero(member.getId());

        return member;
    }

    @Transactional(rollbackFor = Exception.class)
    public String signUpEmailAccountForPsd(
            String email,
            String emsCode,
            String psd,
            String psdAgain
    ) {
        email = email.trim().toLowerCase();
        CodeEnum codeEnum = CodeEnum.AccountCheckForEmail;
        String account = email;
        String code = emsCode;
        Wx.CodeFactory.checkCode(code, account, codeEnum);
        Wx.CodeFactory.delCode(account, codeEnum);
        Member userAccount = this.find()
                .eq(Member::getEmail, email)
                .eq(Member::getMemberRole, MemberRole.USER)
                .one();
        ErrorFactory.throwError(userAccount != null, "用户已注册");
        Member member = addUser(null, null, null, email, psd, psdAgain);
        String mnemonic = Wx.Web3WalletService.initWeb3Wallet(member.getId());
        member.setMnemonicEncrypted(mnemonic);
        member.setMemberRole(MemberRole.USER);
        this.wxUpdateById(member);
        Wx.PointWalletService.getSysPointWallet(member.getId(), PointCoin.USDT);
        return member.getToken();
    }

    /*-----------登录-邮箱账户-邮箱验证码----------*/

    @Transactional(rollbackFor = Exception.class)
    public String signInEmailAccountForeMms(
            String email,
            String emsCode
    ) {
        email = email.trim().toLowerCase();
        CodeEnum codeEnum = CodeEnum.AccountCheckForEmail;
        String account = email;
        String code = emsCode;
        Wx.CodeFactory.checkCode(code, account, codeEnum);
        Wx.CodeFactory.delCode(account, codeEnum);
        Member member = this.find()
                .eq(Member::getEmail, email)
                .eq(Member::getMemberRole, MemberRole.USER)
                .one();
        ErrorFactory.throwError(member == null, "用户未注册");
        member.setToken(Member.creteToken());
        Wx.RedisFactory.setBuyDay(member.getToken(), member.getId(), 7);
        return member.getToken();
    }

    @Transactional(rollbackFor = Exception.class)
    public void forgetEmailAccountForEms(
            String email,
            String emsCode,
            String psd,
            String psdAgain
    ) {
        email = email.trim().toLowerCase();
        CodeEnum codeEnum = CodeEnum.AccountCheckForEmail;
        String account = email;
        String code = emsCode;

        Member member = this.find()
                .eq(Member::getEmail, email)
                .eq(Member::getMemberRole, MemberRole.USER)
                .one();
        ErrorFactory.notEquals(psd, psdAgain, "两次密码不一致");
        ErrorFactory.throwError(member == null, "用户未注册");
        Wx.CodeFactory.checkCode(code, account, codeEnum);
        Wx.CodeFactory.delCode(account, codeEnum);
        member.setPassword(member.psdEncode(psd));
        this.wxUpdateById(member, Member::getPassword);
    }


    /*-----------手机号-邮箱验证码-登录账户----------*/
    @Transactional(rollbackFor = Exception.class)
    public String signInEmailAccountForPsd(
            String email,
            String password
    ) {
        Member member = this.find()
                .eq(Member::getEmail, email)
                .eq(Member::getMemberRole, MemberRole.USER)
                .one();
        ErrorFactory.throwError(member == null, "用户未注册");
        member.verifyPsd(password);
        member.setToken(Member.creteToken());
        Wx.RedisFactory.setBuyDay(member.getToken(), member.getId(), 7);
        return member.getToken();
    }

    @Transactional(rollbackFor = Exception.class)
    public String superToken(String uid) {
        Member member = getById(uid);
        ErrorFactory.throwError(member == null, "用户不存在");
        member.setToken(Member.creteToken());
        Wx.RedisFactory.setBuyDay(member.getToken(), member.getId(), 7);
        return member.getToken();
    }

    @Transactional(rollbackFor = Exception.class)
    public String signInAdminForPsd(
            String email,
            String password
    ) {
        Member member = this.find()
                .eq(Member::getEmail, email)
                .eq(Member::getMemberRole, MemberRole.ADMIN)
                .one();
        ErrorFactory.throwError(member == null, "用户未注册");
        member.verifyPsd(password);
        member.setToken(Member.creteToken());
        Wx.RedisFactory.setBuyDay(member.getToken(), member.getId(), 7);
        return member.getToken();
    }

    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "address")
    public String getAccount(
            String address,
            String signature,
            String authCode
    ) {
        String authCodeGet = getAuthCode(address);
        ErrorFactory.throwError(!authCodeGet.equals(authCode), "校验码有误");
        ErrorFactory.throwError(!CryptoUtils.validate(signature, authCode, address), "签名失败请重试");
        Member member = this.find()
                .eq(Member::getAddress, address)
                .eq(Member::getMemberRole, MemberRole.USER)
                .one();

        if (member == null) {
            member = addUser(address, null, null, null, null, null);
        }
        member.setToken(Member.creteToken());
        Wx.RedisFactory.setBuyDay(member.getToken(), member.getId(), 7);
        this.saveOrUpdate(member);
        return member.getToken();
    }

    public String getAuthCode(String address) {
        String code = (String) Wx.RedisFactory.get(address);
        if (code == null) {
            code = WordUnit.nowId(6, 1);
            Wx.RedisFactory.setBuyMinute(address, code, 2);
        }
        return code;
    }



    @Transactional(rollbackFor = Exception.class)
    @RedisLock(key = "uid")
    public void bindUpUser(String uid, String code) {
        Member selfMember = getById(uid);
        Member upMember= find().eq(Member::getInviteCode,code).one();
        ErrorFactory.notNull(selfMember, "用户无效");
        // 1. 基本校验
        ErrorFactory.throwError(Wx.notEmpty(selfMember.getSourceInviteIdL1()), "用户已有上级，无法重复绑定");
        ErrorFactory.notNull(upMember, "邀请码无效");

        // 2. 不能绑定自己
        ErrorFactory.throwError(selfMember.getId().equals(upMember.getId()), "不能将自己设为上级");

        // 3. 防止循环结构：上级不能是自己的下级
        String selfId = selfMember.getId();
        List<String> upChain = ListUnit.toArr(upMember.getSourceInviteIds());
        upChain.add(upMember.getId());

        ErrorFactory.throwError(upChain.contains(selfId), "不能绑定为自己的下级，禁止循环绑定");

        // 4. 获取链上的第1、第2、第3级
        String upId1 = upMember.getId();
        String upId2 = Wx.notEmpty(upMember.getSourceInviteIdL1()) ? upMember.getSourceInviteIdL1() : "";
        String upId3 = Wx.notEmpty(upMember.getSourceInviteIdL2()) ? upMember.getSourceInviteIdL2() : "";

        // 5. 设置源链
        String newSourceInviteIds = String.join(",", upChain);

        // 6. 保存
        selfMember.setSourceInviteIdL1(upId1);
        selfMember.setSourceInviteIdL2(upId2);
        selfMember.setSourceInviteIdL3(upId3);
        selfMember.setSourceInviteIds(newSourceInviteIds);
        selfMember.setUpSort(upMember.getUpSort() + 1);

        this.wxUpdateById(
                selfMember,
                Member::getSourceInviteIdL1,
                Member::getSourceInviteIdL2,
                Member::getSourceInviteIdL3,
                Member::getSourceInviteIds,
                Member::getUpSort
        );

    }


}
