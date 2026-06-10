import '../css/imagePreview.css'
import { baseUrl, request } from '#utils/request.js'
import { $, $$ } from '#utils/dom.js'

let stopped = false

const observer = new IntersectionObserver(entries => {
  if(entries[0].isIntersecting) {
    nextImage()
  }
}, {
  rootMargin: '500px'
})
observer.observe($('#sentinel'))

document.addEventListener('DOMContentLoaded', async () => {
  const params = new URLSearchParams(window.location.search)
  const fileId = params.get('fileId')
  if(!fileId) {
    alert('文件不存在')
    return
  }
  const res = await request('/image-preview/show', {
    method: 'POST',
    body: {fileId: fileId}
  })
  showImage(res)
  for(let i=0;i<3;i++){
    await nextImage()
  }

})

async function nextImage() {
  if(stopped) return
  const res = await request('/image-preview/next', {method: 'POST'})
  showImage(res)
}

async function prevImage() {
  if(stopped) return
  const res = await request('/image-preview/prev', {method: 'POST'})
  if(!res || !res.img) return

  const $container = $('#image-preview .image-container')
  const src = `data:${res.fileType};base64,${res.img}`
  const $child = $container.children[$container.children.length - 1]
  $('img', $child).src = src
  $container.prepend($child)
}

function buildImageDiv(src) {
  const $div = document.createElement('div')
  $div.innerHTML = `<img src="${src}" alt="img"/>`
  return $div
}

function showImage(res) {
  if(!res || !res.img) return
  
  const $container = $('#image-preview .image-container')
  const src = `data:${res.fileType};base64,${res.img}`
  let $child
  if($container.children.length < 5) {
    $child = buildImageDiv(src)
  }else{
    $child = $container.children[0]
    $('img', $child).src = src
  }
  $container.appendChild($child)
}
