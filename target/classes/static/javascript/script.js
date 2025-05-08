document.addEventListener('DOMContentLoaded', () => {
  loadDashboard('uploadTime');

  if (document.getElementById('historyTableBody') || document.getElementById('history-section')) {
    loadHistory();
    setInterval(loadHistory, 30000); // Refresh every 30 seconds
  }

  setInterval(() => loadDashboard(document.getElementById('sortFiles')?.value || 'uploadTime'), 30000);
});

function formatDate(dateTimeString) {
  const options = {
    year: 'numeric', month: 'long', day: 'numeric',
    hour: 'numeric', minute: 'numeric', second: 'numeric'
  };
  return new Date(dateTimeString).toLocaleDateString(undefined, options);
}

function timeAgo(date) {
  const now = new Date();
  const seconds = Math.floor((now - new Date(date)) / 1000);
  let interval = Math.floor(seconds / 31536000);
  if (interval >= 1) return interval + ' year(s) ago';
  interval = Math.floor(seconds / 2592000);
  if (interval >= 1) return interval + ' month(s) ago';
  interval = Math.floor(seconds / 86400);
  if (interval >= 1) return interval + ' day(s) ago';
  interval = Math.floor(seconds / 3600);
  if (interval >= 1) return interval + ' hour(s) ago';
  interval = Math.floor(seconds / 60);
  if (interval >= 1) return interval + ' minute(s) ago';
  return 'just now';
}

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

        const actionButton = getActionButton(file);

        const row = document.createElement('tr');
        if (expired) row.classList.add('expired-row');

        row.innerHTML = `
          <td>${file.originalFilename}</td>
          <td>${file.uploaderName}</td>
          <td>${formatDate(file.uploadTime)} <span class="time-ago">(${timeAgo(file.uploadTime)})</span></td>
          <td>${formatDate(file.expiryTime)}</td>
          <td>${statusHTML}</td>
          <td>
            <a href="/download/${file.code}" class="btn-primary btn-sm ${expired ? 'disabled' : ''}">
              Download
            </a>
          </td>
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

function loadHistory() {
  fetch('/api/history')
    .then(response => {
      if (!response.ok) throw new Error('Failed to fetch history data');
      return response.json();
    })
    .then(history => {
      const tableBody = document.getElementById("historyTableBody");
      const historySection = document.getElementById('history-section');

      if (tableBody) tableBody.innerHTML = '';
      if (historySection) historySection.innerHTML = '';

      if (history.length === 0) {
        if (historySection) {
          historySection.innerHTML = '<p class="text-center text-gray-600">No upload history found.</p>';
        }
        return;
      }

      history.sort((a, b) => new Date(b.uploadTime) - new Date(a.uploadTime));

      history.forEach(file => {
        const receiverEmail = file.receiverEmail || file.email || "N/A";
        let status;
        if (file.manuallyDeleted) {
          status = `<span class="text-gray-600 font-semibold">Deleted</span>`;
        } else if (file.downloaded) {
          status = `<span class="text-blue-600 font-semibold">Downloaded</span>`;
        } else if (file.expired) {
          status = `<span class="text-red-600 font-semibold">Expired</span>`;
        } else {
          status = `<span class="text-green-600 font-semibold">Active</span>`;
        }

        const actionButton = getActionButton(file);

        if (tableBody) {
          const tr = document.createElement("tr");
          tr.innerHTML = `
            <td class="px-4 py-2">${file.originalFilename || "Unnamed"}</td>
            <td class="px-4 py-2">${file.uploaderName || "N/A"}</td>
            <td class="px-4 py-2">${receiverEmail}</td>
            <td class="px-4 py-2">${file.uploadTime ? formatDate(file.uploadTime) : "N/A"}</td>
            <td class="px-4 py-2">${status}</td>
            <td class="px-4 py-2">${actionButton}</td>
          `;
          tableBody.appendChild(tr);
        }

        if (historySection) {
          const card = document.createElement('div');
          card.className = "bg-white p-6 rounded-lg shadow-md hover:shadow-lg transition duration-300";

          card.innerHTML = `
            <h3 class="text-lg font-bold mb-2">${file.originalFilename}</h3>
            <p><strong>Uploader:</strong> ${file.uploaderName}</p>
            <p><strong>Receiver Email:</strong> ${receiverEmail}</p>
            <p><strong>Sent Time:</strong> ${new Date(file.uploadTime).toLocaleString()}</p>
            <p><strong>Status:</strong> ${status}</p>
            <div class="mt-4">${actionButton}</div>
          `;

          historySection.appendChild(card);
        }
      });
    })
    .catch(error => {
      console.error("⚠️ Error loading history:", error);
      const historySection = document.getElementById('history-section');
      if (historySection) {
        historySection.innerHTML = `<p class="text-center text-red-600">Error loading upload history. Please try again later.</p>`;
      }
    });
}

function deleteFile(code) {
  if (confirm("Are you sure you want to delete this file?")) {
    fetch(`/delete/${code}`, { method: 'DELETE' })  // Backend-compatible
      .then(res => {
        if (!res.ok) throw new Error("Delete failed");
        alert("File deleted successfully");
        loadHistory();
        loadDashboard('uploadTime');
      })
      .catch(err => {
        console.error("Error deleting file:", err);
        alert("Failed to delete file. Please try again later.");
      });
  }
}

function restoreFile(code) {
  if (confirm("Do you want to restore this file?")) {
    fetch(`/api/cleaned/restore/${code}`, { method: 'PUT' })  // Backend-compatible
      .then(res => {
        if (!res.ok) throw new Error("Restore failed");
        alert("File restored successfully");
        loadHistory();
        loadDashboard('uploadTime');
      })
      .catch(err => {
        console.error("Error restoring file:", err);
        alert("Failed to restore file. Please try again later.");
      });
  }
}

function getActionButton(file) {
  if (file.trashed || file.manuallyDeleted) {
    return `<button onclick="restoreFile('${file.code}')" class="bg-green-500 hover:bg-green-600 text-white px-3 py-1 rounded">Restore</button>`;
  } else {
    return `<button onclick="deleteFile('${file.code}')" class="bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded">Delete</button>`;
  }
}

function displayExpiredFileAlert(expiredFiles) {
  const existingAlert = document.querySelector('.expired-warning');
  if (existingAlert) existingAlert.remove();

  if (expiredFiles.length > 0) {
    let alertBox = document.createElement('div');
    alertBox.classList.add('expired-warning');
	alertBox.innerHTML = `
	     ⚠️ Some of your files have expired. You can no longer access them.
	     <br><br>
	     Please consider uploading a new file if needed.
	   `;

    document.body.appendChild(alertBox);
  }
}

function reUploadFiles() {
  window.location.href = '/upload';
}
