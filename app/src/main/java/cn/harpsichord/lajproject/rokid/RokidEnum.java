package cn.harpsichord.lajproject.rokid;

public class RokidEnum {

    enum RokidStatus {
        s00,  // 刚开始未识别到任何东西，也没有在播放的视频
        s01,  // 识别到场景1-27F前台，视频在播放中
        s10,  // 场景1的视频播放完成，未识别到场景2
        s11,  // 识别到场景2-27F展板1，图文1在播放中
        s12,  // 图文1(BitOne)在播放完之后，播放 “下一个展板” 语音
        s13,  // 开启语音识别
        s14,  // 开始图文2播放
        s15,  // 图文2播放完成
    }

    public static boolean isNewStage(RokidStatus rokidStatus) {
        return (rokidStatus == RokidStatus.s00 || rokidStatus == RokidStatus.s10);
    }

}
