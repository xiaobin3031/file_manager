import { request } from '#utils/request.js'
import { $, $$ } from '#utils/dom.js'
import { buildFileDom } from '#modules/fileDom.js'

let currentFiles = []

export const setFils = (files) => {
  currentFiles = files
}

export const getFiles = (files) => {
  return currentFiles
}

export const loadDirs = async () => {
  let dirs = await request('/ftp/listDirs')
  refreshDirs(dirs)
}

export const refreshDirs = (dirs) => {
  const files = dirs.files || []
  currentFiles = files
  const $app = $('#app')
  while($app.firstChild) {
    $app.removeChild($app.firstChild)
  }

  for(let i=0;i<files.length;i++) {
    const file = files[i]
    $app.appendChild(buildFileDom(file, currentFiles))
  }

  if(dirs.path) {
    const breadText = dirs.path.map(p => {
      return `<span class="path">${p}</span>`
    })
    $('#bread').innerHTML = `/${breadText.join('/')}`
  }
  $$('span.path:not(:last-child)').forEach($path => {
    $path.addEventListener('click', async (e) => {
      const res = await request('/ftp/changeDir', {
        method: 'POST',
        body: {
            dirName: e.currentTarget.textContent
        }
      })
      refreshDirs(res)
    })
  })
}

export const getSelectedFiles = () => {
  const $selectedItems = $$('#app div.file-item.selected')
  const fileIds = Array.from($selectedItems).map(item => +item.dataset.fileId)
  
  return {
      selectedFiles: currentFiles.filter(file => fileIds.includes(file.id)),
      "$selectedItems": $selectedItems
  }
}
