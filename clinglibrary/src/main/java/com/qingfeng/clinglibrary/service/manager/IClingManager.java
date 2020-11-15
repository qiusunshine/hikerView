package com.qingfeng.clinglibrary.service.manager;





import com.qingfeng.clinglibrary.service.ClingUpnpService;

import org.fourthline.cling.registry.Registry;

/**
 * 说明：
 * 作者：zhouzhan
 * 日期：17/6/28 16:30
 */

public interface IClingManager extends IDLNAManager {

    void setUpnpService(ClingUpnpService upnpService);

    void setDeviceManager(IDeviceManager deviceManager);

    Registry getRegistry();
}
