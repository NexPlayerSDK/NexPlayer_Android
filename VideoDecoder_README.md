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
```java
decoder.start();
```
6. MediaCodec에서 사용할 input/output buffer 생성 및 초기화.<br>
Android L에선 초기화 과정의 getInput/OutputBuffers() API를 이용하여 buffer를 얻는 부분이 deprecated 됐고 getInput/OutputBuffer() API를 이용합니다.
```java
ByteBuffer[] inputBuffers = null;
ByteBuffer[] outputBuffers = null;

if (Build.VERSION.SDK_INT < 21) {
  inputBuffers = decoder.getInputBuffers();
  outputBuffers = decoder.getOutputBuffers();
}
```
7. 파서에서 얻은 FrameData를 Decoder에 요청하여 InputQueue를 획득한 후, FrameData를 Buffer에 넣고 Decoder에 전달합니다.
```java
int inputIndex = decoder.dequeueInputBuffer(5000);
if (inputIndex >= 0) {
  ByteBuffer buffer = null;
  if (Build.VERSION.SDK_INT >= 21) {
    buffer = decoder.getInputBuffer(inputIndex);
  else
    buffer = inputBuffers[inputIndex];
  
  int readFrameSize = extractor.readSampleData(buffer, 0);
  if (readFrameSize < 0) {
    //EOF
    decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
  } else {
    decoder.queueInputBuffer(inputIndex, 0, readFrameSize, extractor.getSampleTime(), 0);
    extractor.advance();
  }
} else {
  Log.d("MediaCodec", "timeout...!");
}
```
8. Decode된 Output 데이터를 얻기위한 처리를 합니다.
```java
BufferInfo OutputInfo = new BufferInfo();
int outputIndex = decoder.dequeueOutputBuffer(OutputInfo, 5000);
if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
  Log.d("MediaCodec", "configuration info flag!");

switch (outputIndex) {
  case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
      break;
  case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
      MediaFormat changedFormat = decoder.getOutputFormat();
      Log.d("MediaCodec", "Format changed info : mime=" + changedFormat.getString(MediaFormat.KEY_MIME) + ", with=" + changedFormat.getInteger(MediaFormat.KEY_WIDTH) + ", height=" + changedFormat.getInteger(MediaFormat.KEY_HEIGHT));
      break;
  case MediaCodec.INFO_TRY_AGAIN_LATER:
      //this mean is timeout
      break;
  case default:
      ByteBuffer buffer;
      if (Build.VERSION.SDK_INT >= 21)
        buffer = decoder.getOutputBuffer(outputIndex);
      else
        buffer = outputBuffers[outputIndex];
      
      //only testing
      try {
        sleep(33);
      } catch (InterruptedException e) {
        e.printStackTrace();
        break;
      }
      
      if (Build.VERSION.SDK_INT >= 21) {
        long nanoTime = OutputInfo.presentationTimeUs * 1000;
        Log.d("MediaCodec", "Rendering output buffer idx=" + outputIndex + ", Render PTS=" + OutputInfo.presentationTimeUs / 1000 + ", nanoTime=" + nanoTime);
        decoder.releaseOutputBuffer(outputIndex, nanoTime);
      } else {
        decoder.releaseOutputBuffer(outputIndex, true);
      }
      break;
  }
```

### Android BufferQueue System
Android Graphics System은 BufferQueue라는 핵심 클래스에 의해서 Data를 핸들링합니다. 이것의 역할은 아주 단순합니다. 그래픽 버퍼를 생성하는 컴포넌트<b>(생산자)</b>와 이 데이터를 받아서 디스플레이 하거나 프로세싱하는 컴포넌트<b>(소비자)</b>를 연결시켜 줍니다. 이러한 생산자/소비자 사이의 데이터를 이동시키는 작업을 BufferQueue를 통해서 처리합니다.<br>
위 코드를 보면 BufferQueue system에 의해서 dequeue/queue API를 이용하여 Frame 데이터 및 Rendering 데이터를 서로 유기적으로 사용하고 있는 것을 볼 수 있습니다.
  사용방법은 간단합니다.
  1. 생산자는 일련의 버퍼특징을 기술하여 비어있는 버퍼를 요청합니다 ==> dequeueInputBuffer
  2. 생산자는 버퍼를 채운다음 이를 다시 Queue에 반환합니다. ==> queueInputBuffer
  3. 프로세싱이 끝난 후, 소비자는 버퍼를 획득합니다. ==> acquireBuffer/dequeueOutputBuffer
  4. 소비자는 획득한 버퍼의 데이터를 사용합니다.
  5. 사용이 완료 됐으면 소비자는 반환합니다. ==> releaseOutputBuffer
  좀 더 자세한 사항은 아래 URL을 참고하기 바랍니다.<p>
  <a href=http://source.android.com/devices/graphics/architecture.html>Android Graphics Architecture</a>
