console.log(document.readyState)
if(document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', documentInit)
}else{
  documentInit()
}

async function documentInit() {
  const $main = $('body > div.\\[grid-area\\:main\\] > div')
  const $head = $main.children[0]
  const $btn = downloadCurrent($head, $main)
  const $downloadAllBtn = downloadAllBtn($btn, $main)
  const $downloadPrevBtn = downloadAllPrevBtn($btn, $main)
  $head.appendChild($downloadPrevBtn)
  $head.appendChild($btn)
  $head.appendChild($downloadAllBtn)

  if(localStorage.getItem('continuous') === "1") {
    await sleep(3)
    $downloadAllBtn.click()
  }else if(localStorage.getItem('continuous') === "2") {
    await sleep(3)
    $downloadPrevBtn.click()
  }
}

function downloadCurrent($head, $main) {
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
        let filename = url.substring(url.lastIndexOf('/') + 1)
        const idx = filename.indexOf('.');
        const ext = filename.substring(idx)
        filename = filename.substring(0, idx)
        if(filename.indexOf('-') > 0) {
          filename = filename.split('-')[0]
          filename = '000' + filename
          filename = filename.substring(filename.length - 3)
        }
        filename += ext
        return {
          url,
          filename 
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
  return $btn
}

function waitUntilDownloadFinish($btn) {
  return new Promise(async (resolve) => {
    while(true) {
      await sleep(1)
      if($btn.disabled) {
        continue
      }
      resolve()
      break
    }
  })
}

function downloadAllBtn($downloadBtn, $main) {
  const $btn = document.createElement('button')
  $btn.type = 'button'
  $btn.innerText = '下载所有(下一话)'
  $btn.addEventListener('click', async e => {
    localStorage.setItem('continuous', 1)
    try {
      $downloadBtn.click()
      await waitUntilDownloadFinish($downloadBtn)
      const $nextEle = $main.children[$main.children.length - 1].children[1].children[0].children[2]
      if($nextEle.tagName === 'A') {
        $nextEle.click()
      }else{
        $btn.disabled = true
        $btn.style.color = 'green'
        $btn.innerText = '下载完成'
        localStorage.removeItem('continuous')
      }
    }catch(e) {
      console.log('翻页失败', e)
      localStorage.removeItem('continuous')
    }
  })
  return $btn
}

function downloadAllPrevBtn($downloadBtn, $main) {
  const $btn = document.createElement('button')
  $btn.type = 'button'
  $btn.innerText = '下载所有(上一话)'
  $btn.addEventListener('click', async e => {
    localStorage.setItem('continuous', 2)
    try {
      $downloadBtn.click()
      await waitUntilDownloadFinish($downloadBtn)
      const $prevBtn = $main.children[$main.children.length - 1].children[1].children[0].children[0]
      if($prevBtn.tagName === 'A') {
        $prevBtn.click()
      }else{
        $btn.disabled = true
        $btn.style.color = 'green'
        $btn.innerText = '下载完成'
        localStorage.removeItem('continuous')
      }
    }catch(e) {
      console.log('翻页失败', e)
      localStorage.removeItem('continuous')
    }
  })
  return $btn
}
