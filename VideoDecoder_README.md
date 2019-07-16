# Video Decoder in NexPlayer™ for Android
이 항목에서는 NexPlayer에서 사용되는 Video Decoder에 대해서 다룹니다.
<hr />

## Android MediaCodec을 이용한 Video Decoding 방법.
NexPlayer는 libnexcralbody_mc_jb.so 파일에 Android MediaCodec class를 사용한 native code의 구현이 있습니다.

MediaCodec을 사용하기 위해서는 Android의 MediaFormat, MediaCodecInfo, Surface 등의 여러 class들과 유기적으로 매칭 되어야 하며 MediaServer process의 제어를 받게 됩니다.

이 문서에서는 NexPlayer의 Codec Adaptation Layer(CAL body)에서 구현한 native code를 java layer에서 구현한 simple code를 설명하여 MediaCodec의 동작 흐름을 쉽게 이해하는데 목적이 있습니다.
<hr />

### Android MediaCodec 사용방법.
* Sample code에서는 Android MediaExtractor Parser를 사용하여 설명하며 부분적으로 sample code를 추가했고 repository에 full source를 올려놨습니다.
1. 파서생성 및 입력소스 설정.
```java
private static final String SAMPLE = Environment.getExternalStorageDirectory()+"/Download/Sintel_1080p.mp4";
private MediaExtractor extractor;

extractor = new MediaExtractor();
try {
  extractor.setDataSource(SAMPLE_URL);
} catch {IOException e) {
  e.printStackTrace();
}
```
2. MediaCodec 초기화를 위해서 재생할 컨텐츠의 MediaFormat 정보를 얻어야 합니다. MediaExtractor에서 track 정보를 얻어와 해당 track의 MediaFormat을 구합니다. 여기에선 video channel만 사용하도록 MediaExtractor에 track selection 해줍니다.
```java
String mime = null;

for (int i = 0; i < extractor.getTrackCount(); i++) {
  MediaFormat format = extractor.getTrackFormat(i);
  mime = format.getString(MediaFormat.KEY_MIME);
  
  Lodg.d("MediaCodec", "mime : " + mime);
  
  if (mime.startsWith("video/")) {
    extractor.selectTrack(i);
    break;
  }
}
```
3. MediaFormat을 얻은 후, 생성될 codec의 mime type을 구했으면 MediaCodec을 생성합니다. decoder를 생성할 때는 mime type으로 생성하는 방식이 있고 decoder name으로 생성하는 방법<b>(createDecoderByName)</b>이 있습니다. NexPlayer의 Codec Adaptation Layer의 구현은 Name으로 생성하게 되어 있으며 Name으로 구하기 위해서는 mime type과 <b>MediaCodecInfo</b> class를 이용하여 decoder name을 searching 하여 구할 수 있습니다. 자세한 사항은 <b>Appendix section</b>에 추가되어 있습니다.
```java
private MediaCodec decoder;
try {
  decoder = MediaCodec.createDecoderByType(mime);
} catch (IOException e) {
  e.printStackTrace();
}
```
4. 생성된 MediaCodec에 재생될 컨텐츠의 format 정보 및 출력될 output(surface), drm정보 등을 <b>configure()</b> api로 설정합니다.
```java
@override
public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  if (mPlayer == null) {
    mPlayer = new PlayerThread(holder.getSurface());
    mPlayer.start();
  }
}

private class PlayerThread extends Thread {
  public PlayerThread(Surface surface) {
    this.surface = surface;
  }
        .
        .
        .
@override
public void run() {
        .
        .
  decoder.configure(format, surface, null, 0);
        .
        .
        .
```
5. MediaCodec 시작.
