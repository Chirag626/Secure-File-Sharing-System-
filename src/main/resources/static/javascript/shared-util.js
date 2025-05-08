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

function deleteFile(code) {
  if (confirm("Are you sure you want to delete this file?")) {
    fetch(`/delete/${code}`, { method: 'DELETE' })
      .then((res) => {
        if (res.ok) {
          alert("File deleted successfully");
          // Reload history and dashboard if the deletion was successful
          loadHistory?.();
          //loadDashboard?.('uploadTime');
        } else {
          return res.json().then((data) => {
            // Handle server error message if provided
            throw new Error(data.message || "Failed to delete file");
          });
        }
      })
      .catch((err) => {
        console.error("Error deleting file:", err);
        alert(err.message || "Failed to delete file. Please try again later.");
      });
  }
}

function restoreFile(code) {
  if (confirm("Do you want to restore this file?")) {
    fetch(`/api/cleaned/restore/${code}`, { method: 'PUT' })
      .then(res => {
        if (!res.ok) throw new Error("Restore failed");
        alert("File restored successfully");
        loadHistory?.();
        loadDashboard?.('uploadTime');
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
