document.addEventListener('DOMContentLoaded', () => {
  const savedFilter = localStorage.getItem('selectedHistoryFilter') || 'all';
  if (document.getElementById('historyTableBody') || document.getElementById('history-section')) {
    loadHistory(savedFilter);
    setInterval(() => loadHistory(localStorage.getItem('selectedHistoryFilter') || 'all'), 30000);
  }
});

function loadHistory(filter = 'all') {
  // Save selected filter
  localStorage.setItem('selectedHistoryFilter', filter);

  // Highlight selected button
  document.querySelectorAll('.filter-btn').forEach(btn => {
    btn.classList.remove('ring', 'ring-offset-2', 'font-bold', 'ring-black');
  });
  const selectedBtn = document.getElementById(`filter-${filter}`);
  if (selectedBtn) {
    selectedBtn.classList.add('ring', 'ring-offset-2', 'font-bold', 'ring-black');
  }

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
        const message = '<p class="text-center text-gray-600">No upload history found.</p>';
        if (historySection) historySection.innerHTML = message;
        return;
      }

      history.sort((a, b) => new Date(b.uploadTime) - new Date(a.uploadTime));

      const filteredHistory = history.filter(file => {
        if (filter === 'active') return !file.expired && !file.downloaded && !file.manuallyDeleted;
        if (filter === 'downloaded') return file.downloaded;
        if (filter === 'expired') return file.expired && !file.downloaded;
        return true; // 'all'
      });

      if (filteredHistory.length === 0) {
        const message = '<p class="text-center text-gray-600">No files found for this filter.</p>';
        if (historySection) historySection.innerHTML = message;
        return;
      }

      filteredHistory.forEach(file => {
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

        // Table row
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

        // Card view
        if (historySection) {
          const card = document.createElement('div');
          card.className = "bg-white p-6 rounded-lg shadow-md hover:shadow-lg transition duration-300 mb-4";

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
