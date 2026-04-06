package org.wx.core.web3unit;


import lombok.Getter;

@Getter
public enum Link {
    BSC(LinkGroup.EVM,"0x3b7Fb24B4F3A8eEA0C234851955c3d534D0aCFD1",56),
    ETH( LinkGroup.EVM,"0x3b7Fb24B4F3A8eEA0C234851955c3d534D0aCFD1",1),
    TRON( LinkGroup.TRON,"TP6tkw8XhDCuyEZFgK65swY47c1kaunfrq",null);

    final LinkGroup group;
    final String rechargeAddress;
    final Integer chainId;
    Link(LinkGroup group,String rechargeAddress,Integer chainId) {
        this.group = group;
        this.rechargeAddress = rechargeAddress;
        this.chainId = chainId;
    }
}