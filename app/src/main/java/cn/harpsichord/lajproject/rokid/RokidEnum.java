package cn.harpsichord.lajproject.rokid;

public class RokidEnum {

    enum RokidStatus {
        s00,  // 刚开始未识别到任何东西，也没有在播放的视频
        s01,  // 识别到场景1-27F前台，视频在播放中
        s10,  // 场景1的视频播放完成，未识别到场景2
    }

    public static boolean isNewStage(RokidStatus rokidStatus) {
        if (rokidStatus == RokidStatus.s00) {
            return true;
        }
        if (rokidStatus == RokidStatus.s10) {
            return true;
        }
        return false;
    }

}
