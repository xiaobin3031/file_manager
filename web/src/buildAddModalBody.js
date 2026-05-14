import { sleep } from "./ftp";
import { $, $$ } from "./util/dom";
import { uploadFile } from "./util/request";

export const buildAddModalBody = () => {
  const $body = document.createElement("div");
  $body.id = "file-add-body";

  const bodyText = `
        <div>
            <label><input type="radio" name="addType" value="1" checked="true" />文件夹</label>
            <label><input type="radio" name="addType" value="2" />文件</label>
            <label><input type="radio" name="addType" value="3" />下载任务</label>
            <label><input type="radio" name="addType" value="4" />下载计划</label>
        </div>
        <div>
            <div class="tab-item tab-1 active">
                <input type="text" placeholder="请输入文件夹名称" />
            </div>
            <div class="tab-item tab-2">
                <div> <input type="file" multiple="multiple" /><button type="button">上传</button></div>
                <div class="file-add-info">
                    <span></span>
                    <span></span>
                    <span class="filename"></span>
                </div>
                <div class="file-add-progress">
                    <div class="progress-bar"></div>
                    <span class="progress-text">0%</span>
                </div>
            </div>
            <div class="tab-item tab-3">
                <textarea placeholder="请输入下载链接"></textarea>
            </div>
            <div class="tab-item tab-4">
                <div>
                    <input id="download-plan-url" type="text" placeholder="请输入下载网址"/>
                    <button type="button" onclick="testDownloadUrl()">测试</button>
                </div>
            </div>
        </div>
    `;

  $body.innerHTML = bodyText;

  Array.from($body.querySelectorAll('input[name="addType"]')).forEach(
    (input) => {
      input.addEventListener("change", (e) => {
        const type = e.target.value;
        $("#file-add-body .tab-item.active")?.classList.remove("active");
        $(`#file-add-body .tab-item.tab-${type}`).classList.add("active");
      });
    },
  );

  $('.tab-2 input[type="file"]', $body).addEventListener("change", (e) => {
    const files = e.target.files;
    const $filename = $(".file-add-info span.filename", $body);
    const $spans = $$(".file-add-info span", $body);
    if (files.length === 0) {
      $spans[0].innerText = "";
      $filename.innerText = "";
      return;
    }
    $spans[0].innerText = `1 / ${files.length}`;
    $filename.innerText = `${files[0].name}`;
    $(".tab-2", $body).classList.add("wait");
  });

  // 上传
  $body.querySelector(".tab-2 button").addEventListener("click", async (e) => {
    const files = $body.querySelector('.tab-2 input[type="file"]').files;
    if (files.length === 0) {
      alert("请选择文件");
      return;
    }
    const $progressBar = $(".file-add-progress .progress-bar", $body);
    const $progressText = $(".file-add-progress .progress-text", $body);
    const $progress = $(".file-add-progress", $body);
    $progressBar.style.width = "0%";
    const $tab2 = $(".tab-2", $body);
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const $spans = $$(".file-add-info span", $body);
      $spans[0].innerText = `${i + 1} / ${files.length}`;
      $spans[1].innerText = `${file.name}`;
      $tab2.classList.replace("wait", "uploading");
      await uploadFile(file, (percent) => {
        if (percent === -1) return;
        let pp = Math.min(percent, 100).toFixed(0);
        $progressBar.style.width = `${pp}%`;
        $progressText.innerText = `${pp}`;
        if (pp === 100) {
          $tab2.classList.replace("uploading", "success");
        }
      });
      await sleep();
      $tab2.classList.replace("success", "wait");
      $progressBar.style.width = "0%";
      await sleep();
    }
  });

  return $body;
};
