import { baseUrl } from '#utils/request.js'
import { $, $$ } from '#utils/dom.js'

let $video

document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search)
  const token = localStorage.getItem('token')
  let src = `${baseUrl}/media/play?fileId=${params.get('fileId')}&token=${token}`
  const start = params.get('start')
  const end = params.get('end')
  const name = params.get('name')
  if(start) src += `&start=${start}`
  if(end) src += `&end=${end}`

  const $videoPlay = $('#video-play')

  $video = $('video', $videoPlay)
  $video.src = src

  $('.video-title span', $videoPlay).innerText = name
})
