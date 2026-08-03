package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBusiness.game.entity.GameRecipe;
import org.wx.core.wxBusiness.game.mapper.GameRecipeMapper;

@Service
public class GameRecipeService extends WxServiceImpl<GameRecipeMapper, GameRecipe> {
}
