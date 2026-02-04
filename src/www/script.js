console.log('LocalServer JavaScript is working!');

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded successfully');
    
    const heading = document.querySelector('h1');
    if (heading) {
        heading.addEventListener('click', function() {
            this.style.transform = 'scale(1.1)';
            setTimeout(() => {
                this.style.transform = 'scale(1)';
            }, 200);
        });
    }
    
    const info = document.createElement('div');
    info.style.marginTop = '30px';
    info.style.fontSize = '0.9em';
    info.style.opacity = '0.8';
    info.innerHTML = `
        <p>✅ HTML Loaded</p>
        <p>✅ CSS Loaded</p>
        <p>✅ JavaScript Loaded</p>
        <p>Server Time: ${new Date().toLocaleString()}</p>
    `;
    document.body.appendChild(info);
});