(function() {
  var script1 = document.createElement('script');
  script1.textContent = `
    var postChatConfig = {
      backgroundColor: "#3e86f6",
      bottom: "16px",
      left: "16px",
      fill: "#FFFFFF",
      width: "44px",
      frameWidth: "375px",
      frameHeight: "600px",
      defaultInput: true,
      upLoadWeb: true,
      showInviteLink: true
    };
  `;
  document.head.appendChild(script1);

  var script2 = document.createElement('script');
  script2.setAttribute('data-postChat_key', '70b649f150276f289d1025508f60c5f58a');
  script2.src = 'https://ai.tianli0.top/static/public/postChatUser.min.js';
  document.head.appendChild(script2);
})();
