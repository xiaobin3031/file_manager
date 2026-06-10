const $ = (selector, $parent=document) => $parent.querySelector(selector)
const $$ = (selector, $parent=document) => $parent.querySelectorAll(selector)

const sleep = (second = 3) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve()
    }, second * 1000)
  })
}
