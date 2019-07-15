# Video Decoder in NexPlayer™ for Android
이 항목에서는 NexPlayer에서 사용되는 Video Decoder에 대해서 다룹니다.
<hr />

## Android MediaCodec을 이용한 Video Decoding 방법.
NexPlayer는 libnexcralbody_mc_jb.so 파일에 Android MediaCodec class를 사용한 native code의 구현이 있습니다.

MediaCodec을 사용하기 위해서는 Android의 MediaFormat, MediaCodecInfo, Surface등의 여러 class들과 유기적으로 매칭되어야 하며 MediaServer process의 제어를 받게 됩니다.

이 문서에서는 NexPlayer의 Codec Adaptation Layer(CAL body)에서 구현한 native code를 java layer에서 구현한 simple code를 설명하여 MediaCodec의 동작 흐름을 쉽게 이해하는데 목적이 있습니다.
<hr />
