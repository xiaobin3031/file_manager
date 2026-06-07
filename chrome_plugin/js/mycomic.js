console.log(document.readyState)
if(document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', documentInit)
}else{
  documentInit()
}

function documentInit() {
  const $main = $('body > div.\\[grid-area\\:main\\] > div')
  const $head = $main.children[0]
  const $btn = document.createElement('button')
  $btn.type = 'button'
  $btn.innerText = '下载图片'
  $btn.addEventListener('click', async e => {
    $btn.disabled = true
    $btn.innerText = '下载中..'
    $btn.style.color = 'red'
    try {
      const foldNames = [
        $head.children[1].innerText.trim(),
        $head.children[2].innerText.trim()
      ]
      const $body = $main.children[1]
      let imgs = Array.from($$('img', $body)).map($img => {
        const url = $img.src || $img.dataset.src
        return {
          url: url,
          fileName: url.substring(url.lastIndexOf('/') + 1)
        }
      })
      chrome.runtime.sendMessage({
        type: 'upload_tmp',
        data: {
          'referer': window.location.href,
          'foldNames': foldNames,
          'items': imgs
        }
      }, r => {
          $btn.disabled = false
          $btn.innerText = '入库完成'
          $btn.style.color = 'green'
        })
    }catch(e) {
      console.log('下载文件失败: ', e)
      $btn.disabled = false
      $btn.innerText = '下载图片'
      $btn.style.color = 'black'
    }
  })
  $head.appendChild($btn)
}
