import '../css/videoPlay.css'
import { baseUrl, request } from '#utils/request.js'
import { $, $$ } from '#utils/dom.js'
import { is_space, is_left, is_right } from '#utils/key_event.js'

let step = 10
let controlHideTimer
let inVideoControl = false
let curFileId
let videoList

let $video
let $videoControl
let $domOverlay
let $videoList

function buildVideoSrc(fileId) {
  const token = localStorage.getItem('token')
  curFileId = fileId
  return `${baseUrl}/media/play?fileId=${fileId}&token=${token}`
}

document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search)
  let src = buildVideoSrc(params.get('fileId'))
  const start = params.get('start')
  const end = params.get('end')
  const name = params.get('name')
  if(start) src += `&start=${start}`
  if(end) src += `&end=${end}`

  const $videoPlay = $('#video-play')

  $video = $('video', $videoPlay)
  $video.src = src

  $('.video-title span', $videoPlay).innerText = name
  $videoControl = $('#video-play .video-control')
  $videoList = $('.video-container .video-list', $videoPlay)
  videoControl()
  videoProgress()
  initImageCrop()
})

function initImageCrop() {

}

document.addEventListener('mousemove', () => {
  if(!$videoControl || inVideoControl) return

  $videoControl.classList.remove('hide')
  clearTimeout(controlHideTimer)
  controlHideTimer = setTimeout(() => {
    $videoControl.classList.add('hide')
  }, 2000)
})

document.addEventListener('keyup', e => {
  if(is_space(e)) {
    if($video.paused) {
      videoPlay()
    }else{
      videoPause()
    }
  }else if(is_left(e)) {
    $video.currentTime = currentTime($video.currentTime - step)
  }else if(is_right(e)) {
    $video.currentTime = currentTime($video.currentTime + step)
  }
})

function currentTime(time) {
  if(time < 0) time = 0
  if(time > $video.duration) time = $video.duration
  return time
}

function videoControl() {
  $videoControl.addEventListener('mouseleave', e => {
    setTimeout(() => {
      e.target.classList.add('hide')
    }, 2000)
  })
  $videoControl.addEventListener('mouseenter', e => {
    inVideoControl = true
    clearTimeout(controlHideTimer)
  })
  $videoControl.addEventListener('mouseleave', () => {
    inVideoControl = false
  })
  $('.btn-play', $videoControl).addEventListener('click', e => {
    e.target.classList.toggle('play')
    if(e.target.classList.contains('play')) {
      $video.play()
    }else{
      $video.pause()
    }
  })

  $('.btn-volume', $videoControl).addEventListener('click', e => {
    e.target.classList.toggle('no-voice')
    if(e.target.classList.contains('no-voice')) {
      $video.muted = true
      $('.volume-silder', $videoControl).value = 0
    }else{
      $video.muted = false
      $('.volume-silder', $videoControl).value = 100
      $video.volume = 1
    }
  })

  $('.btn-fullscreen', $videoControl).addEventListener('click', e => {
    $video.requestFullscreen()
  })

  $('.volume-silder', $videoControl).addEventListener('input', e => {
    $video.volume = e.target.value / 100
    if(e.target.value > 0) {
      $('.btn-volume', $videoControl).classList.remove('no-voice')
      $video.muted = false
    }else{
      $('.btn-volume', $videoControl).classList.add('no-voice')
      $video.muted = true
    }
  })

  $('.btn-next', $videoControl).addEventListener('click', nextVideo)
  $('.btn-previous', $videoControl).addEventListener('click', prevVideo)

  $('.progress-bg', $videoControl).addEventListener('click', e => {
    const rect = e.target.getBoundingClientRect()
    const percent = (e.clientX - rect.left) / rect.width
    $video.currentTime = percent * $video.duration
    videoPlay()
  })

  $('.btn-list', $videoControl).addEventListener('click', async e => {
    if ($videoList.children.length == 0) {
      videoList = await request('/media/video-list', {
        method: 'POST', body: {id: curFileId}
      })
      fillVideoList(videoList, $videoList)
    }
    $videoList.classList.toggle('hide')
  })
}

function fillVideoList(list, $videoList) {
  const $body = list.map(file => {
    return `
      <div class="video-item ${file.id === +curFileId ? 'active': ''}" data-file-id="${file.id}" data-filename="${file.name}">
        <div class="video-sample" style="background: url('${baseUrl}/file/sample/${file.id}.webp') no-repeat center / cover;"></div>
        <div class="video-name">${file.name}</div>
      </div>
    `
  }).join('')
  $videoList.innerHTML = $body

  $videoList.addEventListener('click', e => {
    const target = e.target.parentElement
    if(target.classList.contains('video-item')) {
      const src = buildVideoSrc(target.dataset.fileId)
      videoInit(src, target.dataset.filename)
      $('.active', $videoList).classList.remove('active')
      target.classList.add('active')
      videoPlay()
    }
  })

  $videoList.addEventListener('wheel', e => {
    e.preventDefault()
    $videoList.scrollLeft += e.deltaY
  }, { passive: false })
}

function formatTime(time) {
  if(time < 10) {
    return `0${time}`
  }
  return time + ''
}

function parseTime(time) {
  let hour, minute, second
  second = parseInt(time % 60)
  minute = parseInt(time / 60)
  hour = parseInt(minute / 60)
  if(hour > 0) {
    minute = parseInt(time % 60)
    return `${formatTime(hour)}:${formatTime(minute)}:${formatTime(second)}`
  }else{
    return `${formatTime(minute)}:${formatTime(second)}`
  }
}

function videoProgress() {
  $video.addEventListener('timeupdate', () => {
    const time = $video.currentTime
    const total = $video.duration
    $('.progress-wrapper .progress-current', $videoControl).style.width = `${time / total * 100}%`
    $('.current-time', $videoControl).innerText = parseTime(time)
  })

  $video.addEventListener('loadedmetadata', () => {
    const total = $video.duration
    $('.duration', $videoControl).innerText = parseTime(total)
    videoPlay()
  })

  $video.addEventListener('ended', nextVideo)
}

function videoPlay() {
  $video.play()
  $('.btn-play', $videoControl).classList.add('play') 
  $videoList.classList.add('hide')
}

function videoPause() {
  $video.pause()
  $('.btn-play', $videoControl).classList.remove('play')
}

function videoInit(src, name) {
  videoPause()
  $('.progress-current', $videoControl).style.width = 0
  const oriText = $('.duration', $videoControl).innerText
  let txt
  if(oriText.length > 5) {
    txt = '00:00:00'
  }else{
    txt = '00:00'
  }
  $('.duration', $videoControl).innerText = txt
  $('.current-time', $videoControl).innerText = txt
  if(src) $video.src = src
  $('#video-play .video-title span').innerText = name
}

async function nextVideo() {
  let file = await request('/media/next', {
    method: "POST",
    body: {id: curFileId}
  })
  if(file && file.id) {
    const src = buildVideoSrc(file.id)
    videoInit(src, file.name)
  }
}

async function prevVideo() {
  let file = await request('/media/previous', {
    method: "POST",
    body: {id: curFileId}
  })
  if(file && file.id) {
    const src = buildVideoSrc(file.id)
    videoInit(src, file.name)
  }
}

function videoListDom() {
  if(!$domOverlay) {
    $domOverlay = document.createElement('div')
  }
}
