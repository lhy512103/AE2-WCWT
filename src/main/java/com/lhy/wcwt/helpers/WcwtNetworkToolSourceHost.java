package com.lhy.wcwt.helpers;

/**
 * WCWT 提供的网络工具宿主需要向 AE2 工具箱菜单暴露来源状态。
 */
public interface WcwtNetworkToolSourceHost {
    boolean isCuriosBacked();

    boolean isSourceStillPresent();
}