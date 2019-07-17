# Audio Renderer in NexPlayer™ for Android
이 항목에서는 NexPlayer에서 사용되는 Audio Renderer에 대한 기술에 관한 내용을 다룹니다.

<hr/>

## 어떻게 현재 Timestamp를 구할 수 있을까
nexralbody_audio는 version 4.21.01.15 기준으로 재생 중인 컨텐츠의 현재 position을 구하는 방법은 크게 두가지로 나뉠 수 있습니다.

1. AudioTrack의 getTimestamp API를 활용한 방법
2. AudioTrack의 getPlaybackHeadPosition API를 활용한 방법

<hr/>

### AudioTrack의 getTimestamp API를 활용한 방법

```c++
NXUINT32 getCurrentCTS_kk(int playState, NXINT64 playbackHeadPosition /* != 0 */, NXUINT32 *puCTS) // 04_AudioRender.cpp
```

Kitkat(API => 19)부터 추가된 AudioTrack의 getTimestamp를 활용하여 현재 position을 구하는 방법입니다.
getTimestamp API를 통해 얻을 수 있는 output parameter는 device layer에서 마지막으로 출력된 audio의 position 값 (=audioTimestampPositionFrames)과 출력 시점의 systemTime (nano seconds 기준, =audioTimestampSystemTimeNs)입니다.

android 문서에는 getTimestamp에 대한 정의를 아래와 같이 했습니다.

[getTimestamp documents](https://developer.android.com/reference/android/media/AudioTrack.html#getTimestamp(android.media.AudioTimestamp))

<hr/>

**getTimestamp를 통해 현재 position을 구할 때 고려해야하는 3가지 경우가 있습니다.**

1. getTimestamp의 return이 false 인 경우
playbackHeadPosition을 현재 position으로 설정합니다. 
그러나 대개 playbackHeadPosition값은 증가되지 않습니다. NexPlayer는 이 함수를 통해서 Audio/Video Sync를 맞추므로 화면이 멈추어 있습니다.

2. getTimestamp의 return이 true 이면서, audioTimestampPositionFrames 값이 처음 획득한 값인 경우
audioTimestampPositionFrames을 현재 position으로 설정합니다. 그러나 약간의 시간동안은 audioTimestampPositionFrames값이 증가되지 않을 수 있습니다. 이 때에도 화면이 멈추어 있습니다.
최초로 획득한 audioTimestampPositionFrames을 initialTimestampPositionFrames 변수에 설정합니다.

3. getTimestamp의 return이 true 이면서, audioTimestampPositionFrames 값이 처음 획득한 값과 다른 경우
아래의 조건이 만족하는 경우 timestampAdvancing을 true 설정합니다. 

```c++
if (initializedTimestamp)
{
    if (UNSET_FRAME_POSITION != initialTimestampPositionFrames && initialTimestampPositionFrames < audioTimestampPositionFrames)
    {
        if (!timestampAdvancing)
        {
            timestampAdvancing = true;            
        }
    }
    ...
    ...
}
```

**timestampAdvancing이 활성화 되기 시작할 때부터는 getTimestamp값을 자주 호출하지 않도록 조정합니다.**
구글 개발 문서에서는 getTimestamp값을 자주 호출하는 경우, 효과적이지 않으며 실제 퍼포먼스 문제를 유발 할 수 있다고 경고합니다. NexPlayer에서는 timestampAdvancing이 활성화 되면 getTimestamp의 호출 간격을 10s로 변경하며 이전까지는 5ms로 사용합니다.

```c++
const NXINT64 FAST_TIMESTAMP_INTERVAL_US = 5000LL /* 5ms */;
const NXINT64 SLOW_TIMESTAMP_INTERVAL_US = 10000000LL /* 10s */;

NXINT64 systemTimestampNs = getMonotonicClockInNanos();
NXINT64 elapsedSinceTimestampUs = (systemTimestampNs - lastAudioTimestampSystemTimeNs) / 1000LL;
NXINT64 audioTimestampPositionFrames = UNSET_TIMESTAMP, audioTimestampSystemTimeNs = UNSET_TIMESTAMP;

NXINT64 sampleIntervalUs = timestampAdvancing ? SLOW_TIMESTAMP_INTERVAL_US : FAST_TIMESTAMP_INTERVAL_US;

bool initializedTimestamp = false;

if (!shouldGetTimestamp && (0x2 /* AudioTrack.PLAYSTATE_PAUSED */ == playState || (lastInitializedTimestamp && (Stopped == currentState || sampleIntervalUs > elapsedSinceTimestampUs))))
{
    initializedTimestamp = lastInitializedTimestamp;
    audioTimestampPositionFrames = lastAudioTimestampPositionFrames;
    audioTimestampSystemTimeNs = lastAudioTimestampSystemTimeNs;

    elapsedSinceTimestampUs = (systemTimestampNs - audioTimestampSystemTimeNs) / 1000LL;
}
```

**getTimestamp가 호출되기 전까지는 audioTimestampSystemTimeNs과 system nanoTime의 elapse time을 계산하여 현재 position을 return 해줍니다.**
```c++
if (timestampAdvancing)
{
    timestampPositionUs += elapsedSinceTimestampUs;
}
```

![NexPlayer AudioRenderer getTimestamp](resources/gettimestamp_01.png)

<hr/>

### 예외 처리

* Kindle fire HDX의 경우 seek 이후의 getTimestamp 값이 일정시간동안 비정상적으로 얻어와질 수 있습니다. 
이 경우, 아래 함수를 통해 getTimestamp값이 정상적으로 구해질 때 까지 PlaybackHeadPosition을 대신하여 사용합니다.

```c++
bool isTrustedTimestamp(initializedTimestamp, audioTimestampPositionFrames, playbackHeadPosition)
```

isTrustedTimestamp 함수에서 getTimestamp의 값이 비정상 값이라고 판단하는 기준은 playbackHeadPosition와 getTimestamp간의 차이입니다.
playbackHeadPosition은 AudioTrack에 write한 값의 Head Position이며 audioTimestampPositionFrames는 출력된 frame position이므로 두 값 사이에는
frame buffer 만큼의 차, 그리고 frame buffer에서 실제 HW로 출력 되기까지의 Latency가 있다고 가정했습니다. NexPlayer에서는 그 차이를 estimatedLatency에 저장했으며, diff factor(default=2)를 통해 max latency를 계산합니다. 즉, playbackHeadPosition와 getTimestamp간의 차가 max latency보다 큰 경우 timestamp의 값을 비정상이라고 판단하고 있습니다.

```c++
NXINT64 latencyMaxUs = (estimatedLatency * diffFactor) * 1000LL;
NXINT64 timestampPositionUs = framesToDurationUs(audioTimestampPositionFrames);
NXINT64 frameUs = framesToDurationUs(playbackHeadPosition);

NXINT64 diffUs = ABS(frameUs - timestampPositionUs);

if (latencyMaxUs < diffUs)
{
    trustedTimestamp = false;
}
```


* 일반적인 과정을 거쳐 계산된 timestamp 값은 이제까지 쓰여진 frame의 total timestamp보다 작아야 합니다. estimatedLatency 만큼의 차이가 있을 것이라 판단하기 때문입니다. 그래서 아래와 같은 예외 처리를 추가했습니다.

```c++
NXINT64 frameUs = framesToDurationUs(getWrittenFrames()) + elapsedSinceTimestampUs;
NXINT64 positionUs = MIN(timestampPositionUs, frameUs);
```


* 또한, 이전의 timestamp보다 커야합니다.

```c++
positionUs = MAX(positionUs, previousPositionUs);
```

<hr/>

### speed control인 경우 timestamp 보정 처리

NexSound를 통해 speed control기능을 활성화 할 경우, timestamp를 speed에 맞추어 보정해야합니다. 관련하여 살펴볼 함수는 아래와 같습니다. 

```c++
NXINT64 applySpeedUp(NXINT64 positionUs)
```

입력 값은 1배속에 해당하는 timestamp 값입니다. speed control을 사용하지 않는 경우, applySpeedUp내의 아래 코드를 타게 됩니다.

```c++
if (1.0f == speed)
{
    return positionUs + playbackParametersOffsetUs - playbackParametersPositionUs;
}
```

위의 코드에서 보정 값으로 사용된 playbackParametersOffsetUs과 playbackParametersPositionUs에 주목하세요. 1배속의 경우 두 값 모두 0으로 원래의 position timestamp에 영향을 끼치지 않습니다. 그러나 배속 조정이 이루어진 이후 다시 1배속으로 설정했을 때, 두 값이 사용됩니다. 

```c++
// setSpeedIfNecessary_l 함수 내의 코드로 consumeBuffer 즉, AudioTrack에 Write하는 곳에 위치한 코드 입니다.
if (currentSpeed != requestedSpeed)
{   		
    AudioTrackTimeInformation playbackParametersCheckpoint;
    
    playbackParametersCheckpoint.speed = numInputSamplesPerChannelRequired / 1024.0f;
    playbackParametersCheckpoint.mediaTimeUs = bufferPresentationTimeUs;
    playbackParametersCheckpoint.positionUs = framesToDurationUs(getWrittenFrames());

    playbackParametersCheckpoints.push(playbackParametersCheckpoint);
}
```

두 보정 값은 배속 command가 입력되어 AudioTrack에 Write 하는 시점의 timestamp들 입니다.
playbackParametersOffsetUs 는 AudioTrack에 Write한 최신의 data의 timestamp 이며, playbackParametersPositionUs는 AudioTrack에 Write한 bytes를 timestamp로 치환한 position 값입니다. 

 그 중, 계산된 현재 position 값보다 작으면서 가장 가까운 값이 보정 값으로 선택됩니다.

```c++
AudioTrackTimeInformation checkpoint;

while (!playbackParametersCheckpoints.empty())
{
    CSALMutex::Auto autolock(lock);
    AudioTrackTimeInformation playbackParametersCheckpoint = playbackParametersCheckpoints.front();

    if (playbackParametersCheckpoint.positionUs <= positionUs)
    {
        playbackParametersCheckpoints.pop();
        checkpoint = playbackParametersCheckpoint;
    }
    else
    {        
        break;
    }
}

if (0 < checkpoint.speed)
{
    speed = currentSpeed = checkpoint.speed;
    playbackParametersPositionUs = checkpoint.positionUs;
    playbackParametersOffsetUs = checkpoint.mediaTimeUs - (firstCTS * 1000L);
}		
```

> 2배속의 경우, playbackParametersOffsetUs는 1배속에 해당하는 timestamp의 2배가 될 것이며 playbackParametersPositionUs는 1배속에 해당하는 timestamp가 될 것이기에 두 값의 차는 배속으로 인해 증감된 timestamp의 보정 값으로 사용될 수 있습니다.

![NexPlayer AudioRenderer SpeedControl](resources/speed_control_01.png)


일반적인 배속 재생을 보정하기 위해서 NexSound에 input으로 들어간 data의 size와 AudioTrack에 write한 output size값을 누적시킵니다.
누적된 inputBytes와 outputBytes를 이용하여 배속 재생 시작 시점부터 현재 position timestamp까지의 값을 아래의 함수를 통해 증감시킵니다.

```c++
NXINT64 scaleLargeTimestamp(NXINT64 timestamp, NXINT64 multiplier, NXINT64 divisor)
```

위의 함수에서 timestamp는 현재의 timestamp에서 배속 재생 시점의 timestamp를 뺀 값이며, multiplier는 input, divisor는 output bytes가 됩니다.
누적된 inputBytes가 큰 경우는 1배속 보다 큰 값으로 보정되며 outputBytes가 큰 경우는 1배속보다 작은 값으로 보정됩니다.
보정되는 방식은 아래와 같습니다.

```c++
NXLDOUBLE multiplicationFactor = (NXLDOUBLE)multiplier / divisor;
(NXINT64)(timestamp * multiplicationFactor);
```

<hr/>

## speed control

NexSound를 통해 speed control기능을 활성화 할 경우, 살펴볼 함수는 아래와 같습니다. 

```c++
void setSpeedIfNecessary_l(long long bufferPresentationTimeUs)
```

speed가 기존과 다를 때, Nexsound에게 request speed(=requestedSpeed)를 입력하고 channel당 input sample size(=numInputSamplesPerChannelRequired)를 받습니다.

```c++
nexSoundHandle->NexSoundSetParam(Processor::SpeedControl, ParamCommand::SpeedControl_PlaySpeed, requestedSpeed);
nexSoundHandle->NexSoundGetParam(Processor::SpeedControl, ParamCommand::SpeedControl_Input_SamplePerChannel, &numInputSamplesPerChannelRequired);
```

numInputSamplesPerChannelRequired 값과 frame size(=numChannel * bitsPerSample >>3)을 곱하면 NexSound에 입력할 input bytes를 계산할 수 있습니다. 

예를 들어,

 channel과 bitsPerSample, 그리고 배속이 각각 2, 16, 1배속인 경우, numInputSamplesPerChannelRequired의 값이 default인 1024 bytes 이며, frame size(=2 * 16 >>3)는 4이므로 NexSound에서 1배속에 필요한 input bytes는 4096이 됩니다. 그러나 2배속으로 변경한 경우, numInputSamplesPerChannelRequired의 값이 NexSound로부터 2048을 return 받아 input size가 8192가 됩니다. 
결과적으로 2배속일 때는 AudioTrack에 decoding된 frame을 write하는 함수들인 consumeBuffer - drainToAudioTrack에서 1배속에 2배에 해당되는 data를 NexSound에 입력합니다. (output size는 1배속과 동일한 size를 가집니다.) 

