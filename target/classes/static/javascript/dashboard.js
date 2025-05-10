document.addEventListener('DOMContentLoaded', () => {
  loadDashboard('uploadTime');
  setInterval(() => loadDashboard(document.getElementById('sortFiles')?.value || 'uploadTime'), 30000);
});

function loadDashboard(sortBy) {
  fetch(`/api/files?sortBy=${sortBy}`)
    .then(response => response.json())
    .then(files => {
      const filesList = document.getElementById('filesList');
      if (!filesList) return;
      filesList.innerHTML = '';

      const now = new Date();
      const expiredFiles = [];

      files.forEach(file => {
        const expiry = new Date(file.expiryTime);
        const timeLeftInMinutes = Math.floor((expiry - now) / 60000);
        const expired = file.expired || timeLeftInMinutes <= 0;

        const statusHTML = expired
          ? `<span class="status expired">Expired</span>`
          : `<span class="status active">Expires in ${timeLeftInMinutes} minute(s)</span>`;

        const row = document.createElement('tr');
        if (expired) row.classList.add('expired-row');

        const downloadButton = expired
          ? `<span class="text-gray-400">Unavailable</span>`
          : `<a href="/download/${file.code}" class="btn-primary btn-sm">Download</a>`;

        const actionButton = getActionButton(file);

        row.innerHTML = `
          <td>${file.originalFilename}</td>
          <td>${file.uploaderName}</td>
          <td>${formatDate(file.uploadTime)} <span class="time-ago">(${timeAgo(file.uploadTime)})</span></td>
          <td>${formatDate(file.expiryTime)}</td>
          <td>${statusHTML}</td>
          <td>${downloadButton}</td>
          <td>${actionButton}</td>
        `;

        filesList.appendChild(row);
        if (expired) expiredFiles.push(file);
      });

      displayExpiredFileAlert(expiredFiles);
    })
    .catch(err => {
      console.error('Failed to load dashboard:', err);
    });
}

function displayExpiredFileAlert(expiredFiles) {
  const existingAlert = document.querySelector('.expired-warning');
  if (existingAlert) existingAlert.remove();

  if (expiredFiles.length > 0) {
    const alertBox = document.createElement('div');
    alertBox.classList.add('expired-warning');
    alertBox.innerHTML = `
      ⚠️ Some of your files have expired. You can no longer access them.
      <br><br>
      Please consider uploading a new file if needed.
    `;
    document.body.appendChild(alertBox);
  }
}
